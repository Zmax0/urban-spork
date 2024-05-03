package com.urbanspork.server.vmess;

import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.CipherMethods;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.codec.vmess.AEADBodyCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.lang.Go;
import com.urbanspork.common.protocol.vmess.Address;
import com.urbanspork.common.protocol.vmess.ID;
import com.urbanspork.common.protocol.vmess.aead.AuthID;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.encoding.ServerSession;
import com.urbanspork.common.protocol.vmess.encoding.Session;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.protocol.vmess.header.RequestHeader;
import com.urbanspork.common.protocol.vmess.header.RequestOption;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import com.urbanspork.common.transport.tcp.RelayingPayload;
import com.urbanspork.common.transport.udp.RelayingPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.urbanspork.common.protocol.vmess.aead.Const.KDF_SALT_AEAD_RESP_HEADER_LEN_IV;
import static com.urbanspork.common.protocol.vmess.aead.Const.KDF_SALT_AEAD_RESP_HEADER_LEN_KEY;
import static com.urbanspork.common.protocol.vmess.aead.Const.KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_IV;
import static com.urbanspork.common.protocol.vmess.aead.Const.KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_KEY;
import static com.urbanspork.common.protocol.vmess.aead.Encrypt.openVMessAEADHeader;

public class ServerAeadCodec extends ByteToMessageCodec<ByteBuf> {

    private final byte[][] keys;
    private RequestHeader header;
    private Session session;
    private PayloadEncoder payloadEncoder;
    private PayloadDecoder payloadDecoder;

    public ServerAeadCodec(ServerConfig config) {
        this(ID.newID(new String[]{config.getPassword()}));
    }

    ServerAeadCodec(byte[][] keys) {
        this.keys = keys;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        if (payloadEncoder == null) {
            byte[] aeadResponseHeaderLengthEncryptionKey = KDF.kdf16(session.getResponseBodyKey(), KDF_SALT_AEAD_RESP_HEADER_LEN_KEY.getBytes());
            CipherMethod cipher = CipherMethods.AES_GCM.get();
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
            payloadEncoder = AEADBodyCodec.getBodyEncoder(header, session);
        }
        if (RequestCommand.UDP.equals(header.command())) {
            payloadEncoder.encodePacket(msg, out);
        } else {
            payloadEncoder.encodePayload(msg, out);
        }
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        InetSocketAddress address = null;
        if (payloadDecoder == null) {
            byte[] authID = new byte[16];
            in.getBytes(0, authID);
            byte[] key = AuthID.match(authID, keys);
            if (key.length == 0) {
                throw new DecoderException("No matched authID");
            }
            ByteBuf decrypted = openVMessAEADHeader(key, in);
            if (!decrypted.isReadable()) {
                return;
            }
            byte[] data = new byte[decrypted.readableBytes() - 4];
            decrypted.getBytes(0, data);
            byte version = decrypted.readByte(); //version
            byte[] requestBodyIV = new byte[16];
            decrypted.readBytes(requestBodyIV);
            byte[] requestBodyKey = new byte[16];
            decrypted.readBytes(requestBodyKey);
            byte responseHeader = decrypted.readByte();
            byte option = decrypted.readByte();
            short b35 = decrypted.readUnsignedByte();
            int paddingLen = b35 >> 4;
            SecurityType security = SecurityType.valueOf((byte) (b35 & 0x0F));
            decrypted.skipBytes(1); // fixed 0
            RequestCommand command = new RequestCommand(decrypted.readByte()); // command
            if (RequestCommand.TCP.equals(command) || RequestCommand.UDP.equals(command)) {
                address = Address.readAddressPort(decrypted);
            }
            decrypted.skipBytes(paddingLen);
            byte[] actual = new byte[Integer.BYTES];
            decrypted.readBytes(actual);
            if (!Arrays.equals(Go.fnv1a32(data), actual)) {
                throw new DecoderException("Invalid auth, but this is a AEAD request");
            }
            header = new RequestHeader(version, command, RequestOption.fromMask(option), security, address, key);
            session = new ServerSession(requestBodyIV, requestBodyKey, responseHeader);
            payloadDecoder = AEADBodyCodec.getBodyDecoder(header, session);
        }
        decode(address, in, out);
    }

    private void decode(InetSocketAddress address, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        List<Object> list = new ArrayList<>();
        boolean isUdp = RequestCommand.UDP.equals(header.command());
        if (isUdp) {
            payloadDecoder.decodePacket(in, list);
        } else {
            payloadDecoder.decodePayload(in, list);
        }
        if (list.isEmpty()) {
            return;
        }
        if (address != null) {
            if (isUdp) {
                list.set(0, new RelayingPacket<>(address, list.getFirst()));
            } else {
                list.set(0, new RelayingPayload<>(address, list.getFirst()));
            }
        }
        out.addAll(list);
    }
}