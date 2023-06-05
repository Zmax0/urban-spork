package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.aead.AEADCipherCodecs;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;
import com.urbanspork.common.codec.vmess.VMessAEADBodyCodec;
import com.urbanspork.common.codec.vmess.VMessAEADHeaderCodec;
import com.urbanspork.common.lang.Go;
import com.urbanspork.common.protocol.vmess.Address;
import com.urbanspork.common.protocol.vmess.ID;
import com.urbanspork.common.protocol.vmess.VMess;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.protocol.vmess.header.RequestOption;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.urbanspork.common.codec.aead.AEADCipherCodec.TAG_SIZE;
import static com.urbanspork.common.protocol.vmess.aead.Const.*;

public class ClientAEADCodec extends ByteToMessageCodec<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(ClientAEADCodec.class);
    private final VMessAEADHeaderCodec headerCodec = new VMessAEADHeaderCodec(AEADCipherCodecs.AES_GCM.get());
    private final ClientSession session = new ClientSession();
    private final byte[] cmdKey;
    private final Socks5CommandRequest address;
    private final SecurityType security;
    private AEADPayloadEncoder bodyEncoder;
    private AEADPayloadDecoder bodyDecoder;

    public ClientAEADCodec(String uuid, Socks5CommandRequest address, SupportedCipher cipher) {
        this(ID.newID(uuid), address, SecurityType.valueOf(cipher));
    }

    ClientAEADCodec(byte[] cmdKey, Socks5CommandRequest address, SecurityType security) {
        this.cmdKey = cmdKey;
        this.address = address;
        this.security = security;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (bodyEncoder == null) {
            ByteBuf buffer = out.duplicate();
            buffer.writeByte(VMess.VERSION); // version
            buffer.writeBytes(session.getRequestBodyIV()); // requestBodyIV
            buffer.writeBytes(session.getRequestBodyKey()); // requestBodyKey
            buffer.writeByte(session.getResponseHeader()); // responseHeader
            buffer.writeByte(RequestOption.ChunkStream.getValue() | RequestOption.AuthenticatedLength.getValue()); // option
            int paddingLen = ThreadLocalRandom.current().nextInt(16); // dice roll 16
            buffer.writeByte((paddingLen << 4) | security.getValue());
            buffer.writeByte(0);
            buffer.writeByte(RequestCommand.TCP.getValue());
            Address.writeAddressPort(buffer, address); // address
            if (paddingLen > 0) {
                buffer.writeBytes(Dice.randomBytes(paddingLen)); // padding
            }
            buffer.writeBytes(Go.fnv1a32(ByteBufUtil.getBytes(buffer, buffer.readerIndex(), buffer.writerIndex(), false)));
            byte[] header = new byte[buffer.readableBytes()];
            buffer.readBytes(header);
            headerCodec.sealVMessAEADHeader(cmdKey, header, out);
            bodyEncoder = VMessAEADBodyCodec.getBodyEncoder(security, session);
        }
        bodyEncoder.encodePayload(msg, out);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (bodyDecoder == null) {
            byte[] aeadResponseHeaderLengthEncryptionKey = KDF.kdf16(session.getResponseBodyKey(), KDF_SALT_AEAD_RESP_HEADER_LEN_KEY);
            byte[] aeadResponseHeaderLengthEncryptionIV = KDF.kdf(session.getResponseBodyIV(), headerCodec.cipher().nonceSize(), KDF_SALT_AEAD_RESP_HEADER_LEN_IV);
            byte[] aeadEncryptedResponseHeaderLength = new byte[Short.BYTES + TAG_SIZE];
            in.markReaderIndex();
            in.readBytes(aeadEncryptedResponseHeaderLength);
            int decryptedResponseHeaderLength = Unpooled.wrappedBuffer(headerCodec.cipher().decrypt(aeadResponseHeaderLengthEncryptionKey, aeadResponseHeaderLengthEncryptionIV, null, aeadEncryptedResponseHeaderLength)).readShort();
            if (in.readableBytes() < decryptedResponseHeaderLength + TAG_SIZE) {
                in.resetReaderIndex();
                logger.info("Unexpected readable bytes for decoding client header: expecting {} but actually {}", decryptedResponseHeaderLength + TAG_SIZE, in.readableBytes());
                return;
            }
            byte[] aeadResponseHeaderPayloadEncryptionKey = KDF.kdf16(session.getResponseBodyKey(), KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_KEY);
            byte[] aeadResponseHeaderPayloadEncryptionIV = KDF.kdf(session.getResponseBodyIV(), headerCodec.cipher().nonceSize(), KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_IV);
            byte[] msg = new byte[decryptedResponseHeaderLength + TAG_SIZE];
            in.readBytes(msg);
            ByteBuf encryptedResponseHeaderBuffer = Unpooled.wrappedBuffer(headerCodec.cipher().decrypt(aeadResponseHeaderPayloadEncryptionKey, aeadResponseHeaderPayloadEncryptionIV, null, msg));
            byte responseHeader = encryptedResponseHeaderBuffer.getByte(0);
            if (session.getResponseHeader() != responseHeader) { // v[1]
                logger.error("Unexpected response header: expecting {} but actually {}", session.getResponseHeader(), responseHeader);
                ctx.close();
                return;
            }
            // not support handling command now
            encryptedResponseHeaderBuffer.release();
            bodyDecoder = VMessAEADBodyCodec.getBodyDecoder(security, session);
        }
        bodyDecoder.decodePayload(in, out);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("VMess client codec error", cause);
        ctx.close();
    }

}
