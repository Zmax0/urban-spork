package com.urbanspork.server.vmess;

import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADCipherCodecs;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;
import com.urbanspork.common.codec.vmess.AEADBodyCodec;
import com.urbanspork.common.codec.vmess.AEADHeaderCodec;
import com.urbanspork.common.lang.Go;
import com.urbanspork.common.protocol.vmess.Address;
import com.urbanspork.common.protocol.vmess.ID;
import com.urbanspork.common.protocol.vmess.aead.AuthID;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.encoding.ServerSession;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.protocol.vmess.header.RequestOption;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import static com.urbanspork.common.protocol.vmess.aead.Const.*;

public class ServerAEADCodec extends ByteToMessageCodec<ByteBuf> {

    private final AEADHeaderCodec headerCodec = new AEADHeaderCodec(AEADCipherCodecs.AES_GCM.get());
    private final byte[][] keys;
    private ServerSession session;
    private SecurityType security;
    private AEADPayloadEncoder bodyEncoder;
    private AEADPayloadDecoder bodyDecoder;

    public ServerAEADCodec(String[] uuids) {
        this(ID.newID(uuids));
    }

    ServerAEADCodec(byte[][] keys) {
        this.keys = keys;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        if (bodyEncoder == null) {
            byte[] aeadResponseHeaderLengthEncryptionKey = KDF.kdf16(session.getResponseBodyKey(), KDF_SALT_AEAD_RESP_HEADER_LEN_KEY);
            AEADCipherCodec cipher = headerCodec.cipher();
            int nonceSize = cipher.nonceSize();
            byte[] aeadResponseHeaderLengthEncryptionIV = KDF.kdf(session.getResponseBodyIV(), nonceSize, KDF_SALT_AEAD_RESP_HEADER_LEN_IV);
            byte[] aeadEncryptedHeaderBuffer = new byte[]{session.getResponseHeader(), (byte) RequestOption.AuthenticatedLength.getValue(), 0, 0}; // not support handling command now
            byte[] aeadResponseHeaderLengthEncryptionBuffer = new byte[Short.BYTES];
            Unpooled.wrappedBuffer(aeadResponseHeaderLengthEncryptionBuffer).setShort(0, aeadEncryptedHeaderBuffer.length);
            out.writeBytes(cipher.encrypt(aeadResponseHeaderLengthEncryptionKey, aeadResponseHeaderLengthEncryptionIV, aeadResponseHeaderLengthEncryptionBuffer));
            byte[] aeadResponseHeaderPayloadEncryptionKey = KDF.kdf16(session.getResponseBodyKey(), KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_KEY);
            byte[] aeadResponseHeaderPayloadEncryptionIV = KDF.kdf(session.getResponseBodyIV(), nonceSize, KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_IV);
            out.writeBytes(cipher.encrypt(aeadResponseHeaderPayloadEncryptionKey, aeadResponseHeaderPayloadEncryptionIV, aeadEncryptedHeaderBuffer));
            bodyEncoder = AEADBodyCodec.getBodyEncoder(security, session);
        }
        bodyEncoder.encodePayload(msg, out);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (bodyDecoder == null) {
            byte[] authID = new byte[16];
            in.getBytes(0, authID);
            byte[] key = AuthID.match(authID, keys);
            if (key.length == 0) {
                throw new DecoderException("No matched authID");
            }
            ByteBuf header = headerCodec.openVMessAEADHeader(key, in);
            if (!header.isReadable()) {
                return;
            }
            byte[] data = new byte[header.readableBytes() - 4];
            header.getBytes(0, data);
            header.skipBytes(1); // version
            byte[] requestBodyIV = new byte[16];
            header.readBytes(requestBodyIV);
            byte[] requestBodyKey = new byte[16];
            header.readBytes(requestBodyKey);
            byte responseHeader = header.readByte();
            header.skipBytes(1); // not support handling option now
            short b35 = header.readUnsignedByte();
            int paddingLen = b35 >> 4;
            security = SecurityType.valueOf(b35 & 0x0F);
            header.skipBytes(1); // fixed 0
            byte command = header.readByte(); // command
            if (RequestCommand.TCP.getValue() != command) {
                throw new DecoderException("Not support handling other command now");
            }
            InetSocketAddress address = Address.readAddressPort(header);
            if (paddingLen > 0) {
                header.skipBytes(paddingLen);
            }
            byte[] actual = new byte[Integer.BYTES];
            header.readBytes(actual);
            if (!Arrays.equals(Go.fnv1a32(data), actual)) {
                throw new DecoderException("Invalid auth, but this is a AEAD request");
            }
            session = new ServerSession(requestBodyIV, requestBodyKey, responseHeader);
            bodyDecoder = AEADBodyCodec.getBodyDecoder(security, session);
            out.add(address);
        }
        bodyDecoder.decodePayload(in, out);
    }
}
