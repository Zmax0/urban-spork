package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.CipherCodec;
import com.urbanspork.common.codec.vmess.VMessAEADHeaderCodec;
import com.urbanspork.common.protocol.vmess.VMessProtocol;
import com.urbanspork.common.protocol.vmess.aead.ID;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.cons.AddressType;
import com.urbanspork.common.protocol.vmess.cons.RequestCommand;
import com.urbanspork.common.protocol.vmess.cons.RequestOption;
import com.urbanspork.common.protocol.vmess.cons.SecurityType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ClientCodec extends ByteToMessageCodec<ByteBuf> implements VMessAEADHeaderCodec {

    private static final Logger logger = LoggerFactory.getLogger(ClientCodec.class);

    private final AEADCipher cipher = new GCMBlockCipher(new AESEngine());
    private final Socks5CommandRequest address;
    private final byte[] cmdKey;
    private final ClientSession session;
    private ClientBodyEncoder clientBodyEncoder;
    private ClientBodyDecoder clientBodyDecoder;

    public ClientCodec(String uuid, Socks5CommandRequest address, ClientSession session) {
        this(ID.newID(uuid), address, session);
    }

    public ClientCodec(byte[] cmdKey, Socks5CommandRequest address, ClientSession session) {
        this.cmdKey = cmdKey;
        this.address = address;
        this.session = session;
    }

    @Override
    public AEADCipher cipher() {
        return cipher;
    }

    @Override
    public int macSize() {
        return 128;
    }

    @Override
    public int nonceSize() {
        return 12;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (clientBodyEncoder == null) {
            ByteBuf header = out.alloc().buffer();
            header.writeByte(VMessProtocol.VERSION); // version
            header.writeBytes(session.requestBodyIV); // requestBodyIV
            header.writeBytes(session.requestBodyKey); // requestBodyKey
            header.writeByte(session.responseHeader); // responseHeader
            header.writeByte(RequestOption.ChunkStream.getValue() | RequestOption.AuthenticatedLength.getValue()); // option
            int paddingLen = ThreadLocalRandom.current().nextInt(0, 16); // dice roll 16
            int security = ((paddingLen << 4) | SecurityType.AES128_GCM.getValue());
            header.writeByte(security);
            header.writeByte(0);
            header.writeByte(RequestCommand.TCP.getValue());
            writeAddressBytes(header, address); // address
            if (paddingLen > 0) {
                header.writeBytes(CipherCodec.randomBytes(paddingLen)); // padding
            }
            header.writeBytes(VMessProtocol.fnv1a32(ByteBufUtil.getBytes(header, header.readerIndex(), header.writerIndex(), false)));
            sealVMessAEADHeader(cmdKey, header, out);
            clientBodyEncoder = new ClientBodyEncoder(this, session);
        }
        clientBodyEncoder.encode(ctx, msg, out);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (clientBodyDecoder == null) {
            byte[] aeadResponseHeaderLengthEncryptionKey = KDF.kdf16(session.responseBodyKey, KDF_SALT_AEAD_RESP_HEADER_LEN_KEY);
            byte[] aeadResponseHeaderLengthEncryptionIV = KDF.kdf(session.responseBodyIV, nonceSize(), KDF_SALT_AEAD_RESP_HEADER_LEN_IV);
            byte[] aeadEncryptedResponseHeaderLength = new byte[Short.BYTES + TAG_SIZE];
            in.markReaderIndex();
            in.readBytes(aeadEncryptedResponseHeaderLength);
            int decryptedResponseHeaderLength = Unpooled.wrappedBuffer(decrypt(aeadResponseHeaderLengthEncryptionKey, aeadResponseHeaderLengthEncryptionIV, null, aeadEncryptedResponseHeaderLength)).readShort();
            if (in.readableBytes() < decryptedResponseHeaderLength + TAG_SIZE) {
                in.resetReaderIndex();
                logger.warn("Unable to Read Header Data {}", ctx.channel());
                return;
            }
            byte[] aeadResponseHeaderPayloadEncryptionKey = KDF.kdf16(session.responseBodyKey, KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_KEY);
            byte[] aeadResponseHeaderPayloadEncryptionIV = KDF.kdf(session.responseBodyIV, nonceSize(), KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_IV);
            ByteBuf encryptedResponseHeaderBuffer = in.alloc().buffer(decryptedResponseHeaderLength + TAG_SIZE);
            byte[] msg = new byte[decryptedResponseHeaderLength + TAG_SIZE];
            in.readBytes(msg);
            encryptedResponseHeaderBuffer.writeBytes(decrypt(aeadResponseHeaderPayloadEncryptionKey, aeadResponseHeaderPayloadEncryptionIV, null, msg));
            byte responseHeader = encryptedResponseHeaderBuffer.getByte(0);
            if (session.responseHeader != responseHeader) { // v[1]
                throw new IllegalStateException("Unexpected response header. Expecting " + session.responseHeader + " but actually " + responseHeader);
            }
            encryptedResponseHeaderBuffer.getByte(1);// opt[1]
            encryptedResponseHeaderBuffer.getByte(2); // cmd[1]
            byte cmdLength = encryptedResponseHeaderBuffer.getByte(3);// cmd length M[1]
            if (cmdLength > 0 && encryptedResponseHeaderBuffer.readableBytes() >= 4 + cmdLength) {
                byte[] cmdContent = new byte[cmdLength];
                encryptedResponseHeaderBuffer.getBytes(4, cmdContent, 0, cmdLength); // instruction content[M]
                // TODO handle command
                encryptedResponseHeaderBuffer.readBytes(4 + cmdLength);
            }
            clientBodyDecoder = new ClientBodyDecoder(this, session);
        }
        clientBodyDecoder.decode(ctx, in, out);
    }

    // port[2] + type[1] + domain_len[1] + domain_bytes[n]
    private void writeAddressBytes(ByteBuf buf, Socks5CommandRequest address) {
        int port = address.dstPort();
        byte[] domain = address.dstAddr().getBytes();
        buf.writeShort(port);
        buf.writeByte(AddressType.DOMAIN.getValue());
        buf.writeByte(domain.length);
        buf.writeBytes(domain);
    }

}