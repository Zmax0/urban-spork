package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADCipherCodecs;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;
import com.urbanspork.common.codec.vmess.AEADBodyCodec;
import com.urbanspork.common.codec.vmess.AEADHeaderCodec;
import com.urbanspork.common.lang.Go;
import com.urbanspork.common.protocol.vmess.Address;
import com.urbanspork.common.protocol.vmess.aead.Encrypt;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import com.urbanspork.common.protocol.vmess.encoding.Session;
import com.urbanspork.common.protocol.vmess.header.RequestHeader;
import com.urbanspork.common.protocol.vmess.header.RequestOption;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static com.urbanspork.common.codec.aead.AEADCipherCodec.TAG_SIZE;
import static com.urbanspork.common.protocol.vmess.aead.Const.*;

public class ClientAEADCodec extends ByteToMessageCodec<ByteBuf> implements AEADHeaderCodec {

    private static final Logger logger = LoggerFactory.getLogger(ClientAEADCodec.class);
    private final Encrypt encrypt = new Encrypt(AEADCipherCodecs.AES_GCM.get());
    private final RequestHeader header;
    private final Session session;
    private AEADPayloadEncoder bodyEncoder;
    private AEADPayloadDecoder bodyDecoder;

    public ClientAEADCodec(SupportedCipher cipher, Socks5CommandRequest address, String uuid) {
        this(SecurityType.valueOf(cipher), address, uuid);
    }

    ClientAEADCodec(SecurityType security, Socks5CommandRequest address, String uuid) {
        this(RequestHeader.defaultHeader(security, address, uuid), new ClientSession());
    }

    ClientAEADCodec(RequestHeader header, Session session) {
        this.header = header;
        this.session = session;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (bodyEncoder == null) {
            encodeRequest(header, session, out);
            bodyEncoder = AEADBodyCodec.getBodyEncoder(header.security(), session);
        }
        bodyEncoder.encodePayload(msg, out);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (bodyDecoder == null) {
            if (decodeRequest(in).isPresent()) {
                bodyDecoder = AEADBodyCodec.getBodyDecoder(header.security(), session);
                bodyDecoder.decodePayload(in, out);
            }
        } else {
            bodyDecoder.decodePayload(in, out);
        }
    }

    @Override
    public void encodeRequest(RequestHeader header, Session session, ByteBuf out) throws InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        ByteBuf buffer = out.duplicate();
        buffer.writeByte(header.version()); // version
        buffer.writeBytes(session.getRequestBodyIV()); // requestBodyIV
        buffer.writeBytes(session.getRequestBodyKey()); // requestBodyKey
        buffer.writeByte(session.getResponseHeader()); // responseHeader
        buffer.writeByte(RequestOption.toMask(header.option())); // option
        int paddingLen = ThreadLocalRandom.current().nextInt(16); // dice roll 16
        SecurityType security = header.security();
        buffer.writeByte((paddingLen << 4) | (int) security.getValue());
        buffer.writeByte(0);
        buffer.writeByte(header.command().getValue());
        Address.writeAddressPort(buffer, header.address()); // address
        buffer.writeBytes(Dice.randomBytes(paddingLen)); // padding
        buffer.writeBytes(Go.fnv1a32(ByteBufUtil.getBytes(buffer, buffer.readerIndex(), buffer.writerIndex(), false)));
        byte[] headerBytes = new byte[buffer.readableBytes()];
        buffer.readBytes(headerBytes);
        encrypt.sealVMessAEADHeader(header.id(), headerBytes, out);
    }

    @Override
    public Optional<DecodeResult> decodeRequest(ByteBuf in) throws InvalidCipherTextException {
        AEADCipherCodec cipher = encrypt.cipher();
        int nonceSize = cipher.nonceSize();
        byte[] aeadResponseHeaderLengthEncryptionKey = KDF.kdf16(session.getResponseBodyKey(), KDF_SALT_AEAD_RESP_HEADER_LEN_KEY.getBytes());
        byte[] aeadResponseHeaderLengthEncryptionIV = KDF.kdf(session.getResponseBodyIV(), nonceSize, KDF_SALT_AEAD_RESP_HEADER_LEN_IV.getBytes());
        byte[] aeadEncryptedResponseHeaderLength = new byte[Short.BYTES + TAG_SIZE];
        in.markReaderIndex();
        in.readBytes(aeadEncryptedResponseHeaderLength);
        int decryptedResponseHeaderLength = Unpooled.wrappedBuffer(cipher.decrypt(aeadResponseHeaderLengthEncryptionKey,
            aeadResponseHeaderLengthEncryptionIV, aeadEncryptedResponseHeaderLength)).readShort();
        if (in.readableBytes() < decryptedResponseHeaderLength + TAG_SIZE) {
            in.resetReaderIndex();
            logger.info("Unexpected readable bytes for decoding client header: expecting {} but actually {}", decryptedResponseHeaderLength + TAG_SIZE, in.readableBytes());
            return Optional.empty();
        }
        byte[] aeadResponseHeaderPayloadEncryptionKey = KDF.kdf16(session.getResponseBodyKey(), KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_KEY.getBytes());
        byte[] aeadResponseHeaderPayloadEncryptionIV = KDF.kdf(session.getResponseBodyIV(), nonceSize, KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_IV.getBytes());
        byte[] encryptedResponseHeaderBytes = new byte[decryptedResponseHeaderLength + TAG_SIZE];
        in.readBytes(encryptedResponseHeaderBytes);
        byte[] decryptedResponseHeaderBytes = cipher.decrypt(aeadResponseHeaderPayloadEncryptionKey, aeadResponseHeaderPayloadEncryptionIV, encryptedResponseHeaderBytes);
        byte responseHeader = decryptedResponseHeaderBytes[0];
        if (session.getResponseHeader() != responseHeader) { // v[1]
            throw new DecoderException(String.format("Unexpected response header: expecting %d but actually %d", session.getResponseHeader(), responseHeader));
        }
        // not support handling command now -> decryptedResponseHeaderBytes[1]
        return Optional.of(new DecodeResult(header, session));
    }
}
