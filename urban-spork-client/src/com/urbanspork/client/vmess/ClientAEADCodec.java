package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADCipherCodecs;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;
import com.urbanspork.common.codec.vmess.VMessAEADHeaderCodec;
import com.urbanspork.common.lang.Go;
import com.urbanspork.common.protocol.vmess.ID;
import com.urbanspork.common.protocol.vmess.VMess;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.cons.AddressType;
import com.urbanspork.common.protocol.vmess.cons.RequestCommand;
import com.urbanspork.common.protocol.vmess.cons.RequestOption;
import com.urbanspork.common.protocol.vmess.cons.SecurityType;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static com.urbanspork.common.codec.aead.AEADCipherCodec.TAG_SIZE;
import static com.urbanspork.common.codec.vmess.VMessAEADHeaderCodec.*;

abstract class ClientAEADCodec extends ByteToMessageCodec<ByteBuf> implements Supplier<AEADCipherCodec> {

    private static final Logger logger = LoggerFactory.getLogger(ClientAEADCodec.class);

    final ClientSession session;
    private final byte[] cmdKey;
    private final Socks5CommandRequest address;
    private final VMessAEADHeaderCodec headerCodec = new VMessAEADHeaderCodec(AEADCipherCodecs.AES_GCM.get());
    private final SupportedCipher bodyCipher;
    private AEADPayloadEncoder bodyEncoder;
    private AEADPayloadDecoder bodyDecoder;

    protected ClientAEADCodec(String uuid, Socks5CommandRequest address, ClientSession session, SupportedCipher cipher) {
        this(ID.newID(uuid), address, session, cipher);
    }

    protected ClientAEADCodec(byte[] cmdKey, Socks5CommandRequest address, ClientSession session, SupportedCipher cipher) {
        this.cmdKey = cmdKey;
        this.address = address;
        this.session = session;
        this.bodyCipher = cipher;
    }

    protected abstract AEADPayloadEncoder newClientBodyEncoder();

    protected abstract AEADPayloadDecoder newClientBodyDecoder();

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (bodyEncoder == null) {
            ByteBuf buffer = out.duplicate();
            buffer.writeByte(VMess.VERSION); // version
            buffer.writeBytes(session.requestBodyIV); // requestBodyIV
            buffer.writeBytes(session.requestBodyKey); // requestBodyKey
            buffer.writeByte(session.responseHeader); // responseHeader
            buffer.writeByte(RequestOption.ChunkStream.getValue() | RequestOption.AuthenticatedLength.getValue()); // option
            int paddingLen = ThreadLocalRandom.current().nextInt(16); // dice roll 16
            int security = (paddingLen << 4) | SecurityType.from(bodyCipher).getValue();
            buffer.writeByte(security);
            buffer.writeByte(0);
            buffer.writeByte(RequestCommand.TCP.getValue());
            writeAddressBytes(buffer, address); // address
            if (paddingLen > 0) {
                buffer.writeBytes(Dice.randomBytes(paddingLen)); // padding
            }
            buffer.writeBytes(Go.fnv1a32(ByteBufUtil.getBytes(buffer, buffer.readerIndex(), buffer.writerIndex(), false)));
            byte[] header = new byte[buffer.readableBytes()];
            buffer.readBytes(header);
            headerCodec.sealVMessAEADHeader(cmdKey, header, out);
            bodyEncoder = newClientBodyEncoder();
        }
        bodyEncoder.encodePayload(msg, out);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (bodyDecoder == null) {
            byte[] aeadResponseHeaderLengthEncryptionKey = KDF.kdf16(session.responseBodyKey, KDF_SALT_AEAD_RESP_HEADER_LEN_KEY);
            byte[] aeadResponseHeaderLengthEncryptionIV = KDF.kdf(session.responseBodyIV, headerCodec.codec().nonceSize(), KDF_SALT_AEAD_RESP_HEADER_LEN_IV);
            byte[] aeadEncryptedResponseHeaderLength = new byte[Short.BYTES + TAG_SIZE];
            in.markReaderIndex();
            in.readBytes(aeadEncryptedResponseHeaderLength);
            int decryptedResponseHeaderLength = Unpooled.wrappedBuffer(headerCodec.codec().decrypt(aeadResponseHeaderLengthEncryptionKey, aeadResponseHeaderLengthEncryptionIV, null, aeadEncryptedResponseHeaderLength)).readShort();
            if (in.readableBytes() < decryptedResponseHeaderLength + TAG_SIZE) {
                in.resetReaderIndex();
                logger.info("Unexpected readable bytes for decoding client header: expecting {} but actually {}", decryptedResponseHeaderLength + TAG_SIZE, in.readableBytes());
                return;
            }
            byte[] aeadResponseHeaderPayloadEncryptionKey = KDF.kdf16(session.responseBodyKey, KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_KEY);
            byte[] aeadResponseHeaderPayloadEncryptionIV = KDF.kdf(session.responseBodyIV, headerCodec.codec().nonceSize(), KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_IV);
            byte[] msg = new byte[decryptedResponseHeaderLength + TAG_SIZE];
            in.readBytes(msg);
            ByteBuf encryptedResponseHeaderBuffer = Unpooled.wrappedBuffer(headerCodec.codec().decrypt(aeadResponseHeaderPayloadEncryptionKey, aeadResponseHeaderPayloadEncryptionIV, null, msg));
            byte responseHeader = encryptedResponseHeaderBuffer.getByte(0);
            if (session.responseHeader != responseHeader) { // v[1]
                logger.error("Unexpected response header: expecting {} but actually {}", session.responseHeader, responseHeader);
                ctx.close();
                return;
            }
            // not support handling command now
            encryptedResponseHeaderBuffer.release();
            bodyDecoder = newClientBodyDecoder();
        }
        bodyDecoder.decodePayload(in, out);
    }

    @Override
    public AEADCipherCodec get() {
        return headerCodec.codec();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("VMess client codec error", cause);
        ctx.close();
    }

    // port[2] + type[1] + domain_len[1] + domain_bytes[n]
    private void writeAddressBytes(ByteBuf buf, Socks5CommandRequest address) {
        buf.writeShort(address.dstPort());
        Socks5AddressType addressType = address.dstAddrType();
        String addressString = address.dstAddr();
        if (Socks5AddressType.IPv4.equals(addressType)) {
            if (addressString != null) {
                buf.writeByte(AddressType.IPV4.getValue());
                buf.writeBytes(NetUtil.createByteArrayFromIpAddressString(addressString));
            } else {
                buf.writeInt(0);
            }
        } else if (Socks5AddressType.IPv6.equals(addressType)) {
            if (addressString != null) {
                buf.writeByte(AddressType.IPV6.getValue());
                buf.writeBytes(NetUtil.createByteArrayFromIpAddressString(addressString));
            } else {
                buf.writeLong(0);
                buf.writeLong(0);
            }
        } else if (Socks5AddressType.DOMAIN.equals(addressType)) {
            if (addressString != null) {
                byte[] domain = addressString.getBytes();
                buf.writeByte(AddressType.DOMAIN.getValue());
                buf.writeByte(domain.length);
                buf.writeBytes(domain);
            } else {
                buf.writeByte(0);
            }
        } else {
            throw new EncoderException("Unsupported addressType: " + (addressType.byteValue() & 0xFF));
        }
    }
}
