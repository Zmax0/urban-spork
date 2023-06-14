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
import com.urbanspork.common.protocol.vmess.aead.Encrypt;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.encoding.ServerSession;
import com.urbanspork.common.protocol.vmess.encoding.Session;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.protocol.vmess.header.RequestHeader;
import com.urbanspork.common.protocol.vmess.header.RequestOption;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.urbanspork.common.protocol.vmess.aead.Const.*;

public class ServerAEADCodec extends ByteToMessageCodec<ByteBuf> implements AEADHeaderCodec {

    private final Encrypt encrypt = new Encrypt(AEADCipherCodecs.AES_GCM.get());
    private final byte[][] keys;
    private RequestHeader header;
    private Session session;
    private AEADPayloadEncoder payloadEncoder;
    private AEADPayloadDecoder payloadDecoder;

    public ServerAEADCodec(String[] uuids) {
        this(ID.newID(uuids));
    }

    ServerAEADCodec(byte[][] keys) {
        this.keys = keys;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        if (payloadEncoder == null) {
            encodeRequest(header, session, out);
            payloadEncoder = AEADBodyCodec.getBodyEncoder(header.security(), session);
        }
        payloadEncoder.encodePayload(msg, out);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (payloadDecoder == null) {
            Optional<DecodeResult> optional = decodeRequest(in);
            if (optional.isPresent()) {
                DecodeResult result = optional.get();
                header = result.header();
                session = result.session();
                payloadDecoder = AEADBodyCodec.getBodyDecoder(header.security(), session);
                out.add(header.address());
                payloadDecoder.decodePayload(in, out);
            }
        } else {
            payloadDecoder.decodePayload(in, out);
        }
    }

    @Override
    public void encodeRequest(RequestHeader header, Session session, ByteBuf out) throws InvalidCipherTextException {
        byte[] aeadResponseHeaderLengthEncryptionKey = KDF.kdf16(session.getResponseBodyKey(), KDF_SALT_AEAD_RESP_HEADER_LEN_KEY.getBytes());
        AEADCipherCodec cipher = encrypt.cipher();
        int nonceSize = cipher.nonceSize();
        byte[] aeadResponseHeaderLengthEncryptionIV = KDF.kdf(session.getResponseBodyIV(), nonceSize, KDF_SALT_AEAD_RESP_HEADER_LEN_IV.getBytes());
        int option = RequestOption.toMask(header.option());
        byte[] aeadEncryptedHeaderBuffer = new byte[]{session.getResponseHeader(), (byte) option, 0, 0};
        byte[] aeadResponseHeaderLengthEncryptionBuffer = new byte[Short.BYTES];
        Unpooled.wrappedBuffer(aeadResponseHeaderLengthEncryptionBuffer).setShort(0, aeadEncryptedHeaderBuffer.length);
        out.writeBytes(cipher.encrypt(aeadResponseHeaderLengthEncryptionKey, aeadResponseHeaderLengthEncryptionIV, aeadResponseHeaderLengthEncryptionBuffer));
        byte[] aeadResponseHeaderPayloadEncryptionKey = KDF.kdf16(session.getResponseBodyKey(), KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_KEY.getBytes());
        byte[] aeadResponseHeaderPayloadEncryptionIV = KDF.kdf(session.getResponseBodyIV(), nonceSize, KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_IV.getBytes());
        out.writeBytes(cipher.encrypt(aeadResponseHeaderPayloadEncryptionKey, aeadResponseHeaderPayloadEncryptionIV, aeadEncryptedHeaderBuffer));
    }

    @Override
    public Optional<DecodeResult> decodeRequest(ByteBuf in) throws InvalidCipherTextException {
        byte[] authID = new byte[16];
        in.getBytes(0, authID);
        byte[] key = AuthID.match(authID, keys);
        if (key.length == 0) {
            throw new DecoderException("No matched authID");
        }
        ByteBuf decrypted = encrypt.openVMessAEADHeader(key, in);
        if (!decrypted.isReadable()) {
            return Optional.empty();
        }
        byte[] data = new byte[decrypted.readableBytes() - 4];
        decrypted.getBytes(0, data);
        byte version = decrypted.readByte();// version
        byte[] requestBodyIV = new byte[16];
        decrypted.readBytes(requestBodyIV);
        byte[] requestBodyKey = new byte[16];
        decrypted.readBytes(requestBodyKey);
        byte responseHeader = decrypted.readByte();
        byte option = decrypted.readByte();// not support handling option now
        short b35 = decrypted.readUnsignedByte();
        int paddingLen = b35 >> 4;
        SecurityType security = SecurityType.valueOf((byte) (b35 & 0x0F));
        decrypted.skipBytes(1); // fixed 0
        RequestCommand command = RequestCommand.valueOf(decrypted.readByte());// command
        if (RequestCommand.TCP != command) {
            throw new DecoderException("Not support handling other command now");
        }
        Socks5CommandRequest address = Address.readAddressPort(decrypted);
        decrypted.skipBytes(paddingLen);
        byte[] actual = new byte[Integer.BYTES];
        decrypted.readBytes(actual);
        if (!Arrays.equals(Go.fnv1a32(data), actual)) {
            throw new DecoderException("Invalid auth, but this is a AEAD request");
        }
        return Optional.of(new DecodeResult(
            new RequestHeader(version, command, RequestOption.fromMask(option), security, address, key),
            new ServerSession(requestBodyIV, requestBodyKey, responseHeader)));
    }
}
