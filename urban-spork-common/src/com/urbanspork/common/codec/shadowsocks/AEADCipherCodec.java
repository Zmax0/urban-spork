package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.Authenticator;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.crypto.AES;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.shadowsocks.RequestContext;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
import com.urbanspork.common.protocol.shadowsocks.aead.AEAD;
import com.urbanspork.common.protocol.shadowsocks.aead.AEAD2022;
import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * AEAD Cipher Codec
 *
 * @author Zmax0
 * @see <a href=https://shadowsocks.org/doc/aead.html">AEAD ciphers</a>
 */
class AEADCipherCodec {

    private static final Logger logger = LoggerFactory.getLogger(AEADCipherCodec.class);
    private final byte[] key;
    private final CipherKind cipherKind;
    private final CipherMethod cipherMethod;
    private final byte[] requestSalt;
    private PayloadEncoder payloadEncoder;
    private PayloadDecoder payloadDecoder;
    private long packetId;

    AEADCipherCodec(CipherKind cipherKind, CipherMethod cipherMethod, String password, int saltSize) {
        this.key = cipherKind.isAead2022() ? Base64.getDecoder().decode(password) : AEAD.generateKey(password.getBytes(), saltSize);
        if (key.length != saltSize) {
            String msg = String.format("%s is expecting a %d bytes key, but password: %s (%d bytes after decode)",
                cipherKind, saltSize, password, key.length);
            throw new IllegalArgumentException(msg);
        }
        this.cipherKind = cipherKind;
        this.cipherMethod = cipherMethod;
        this.requestSalt = new byte[saltSize];
    }

    public void encode(RequestContext context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        boolean isAead2022 = cipherKind.isAead2022();
        Socks5CommandRequest request = context.request();
        if (context.network() == Network.UDP) {
            encodePacket(request, isAead2022, msg, out);
        } else {
            if (payloadEncoder == null) {
                initTcpPayloadEncoder(out, isAead2022);
                if (StreamType.Request == context.streamType()) {
                    msg = handleRequestHeader(isAead2022, request, msg, out);
                } else {
                    handleResponseHeader(isAead2022, msg, out);
                }
            }
            payloadEncoder.encodePayload(msg, out);
        }
    }

    private void encodePacket(Socks5CommandRequest request, boolean isAead2022, ByteBuf msg, ByteBuf out)
        throws InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (isAead2022) {
            int paddingLength = AEAD2022.getPaddingLength(msg);
            int nonceLength = AEAD2022.UDP.getNonceLength(cipherKind);
            int tagSize = cipherMethod.tagSize();
            ByteBuf temp = Unpooled.wrappedBuffer(new byte[nonceLength + 8 + 8 + 1 + 8 + 2 + paddingLength + Address.getLength(request) + msg.readableBytes() + tagSize]);
            temp.writerIndex(0);
            temp.writeLong(0); // client_session_id
            temp.writeLong(packetId++); // packet_id
            temp.writeByte(StreamType.Request.getValue());
            temp.writeLong(AEAD2022.now());
            temp.writeShort(paddingLength);
            temp.writeBytes(Dice.rollBytes(paddingLength));
            Address.encode(request, temp);
            temp.writeBytes(msg);
            byte[] nonce = new byte[12];
            temp.getBytes(4, nonce);
            byte[] header = new byte[16];
            temp.readBytes(header);
            out.writeBytes(AES.ECB_NoPadding.encrypt(key, header));
            AEAD2022.UDP.newPayloadEncoder(cipherMethod, key, 0, nonce).encodePacket(temp, out);
        } else {
            byte[] salt = Dice.rollBytes(requestSalt.length);
            out.writeBytes(salt);
            ByteBuf temp = Unpooled.buffer(Address.getLength(request));
            Address.encode(request, temp);
            AEAD.UDP.newPayloadEncoder(cipherMethod, key, salt).encodePacket(Unpooled.wrappedBuffer(temp, msg), out);
            msg.skipBytes(msg.readableBytes());
        }
    }

    private void initTcpPayloadEncoder(ByteBuf out, boolean isAead2022) {
        byte[] salt = Dice.rollBytes(requestSalt.length);
        if (logger.isTraceEnabled()) {
            logger.trace("new request salt {}", Base64.getEncoder().encodeToString(salt));
        }
        out.writeBytes(salt);
        if (isAead2022) {
            payloadEncoder = AEAD2022.TCP.newPayloadEncoder(cipherMethod, key, salt);
        } else {
            payloadEncoder = AEAD.TCP.newPayloadEncoder(cipherMethod, key, salt);
        }
    }

    private ByteBuf handleRequestHeader(boolean isAead2022, Socks5CommandRequest request, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        ByteBuf temp = Unpooled.buffer();
        Address.encode(request, temp);
        if (isAead2022) {
            int paddingLength = AEAD2022.getPaddingLength(msg);
            temp.writeShort(paddingLength);
            temp.writeBytes(Dice.rollBytes(paddingLength));
        }
        temp = Unpooled.wrappedBuffer(temp, msg);
        msg.skipBytes(msg.readableBytes());
        if (isAead2022) {
            for (byte[] bytes : AEAD2022.TCP.newRequestHeader(temp)) {
                out.writeBytes(payloadEncoder.auth().seal(bytes));
            }
        }
        return temp;
    }

    private void handleResponseHeader(boolean isAead2022, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        if (isAead2022) {
            for (byte[] bytes : AEAD2022.TCP.newResponseHeader(requestSalt, msg)) {
                out.writeBytes(payloadEncoder.auth().seal(bytes));
            }
        }
    }

    public void decode(RequestContext header, ByteBuf in, List<Object> out) throws InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (header.network() == Network.UDP) {
            decodePacket(header.streamType(), cipherKind.isAead2022(), in, out);
        } else {
            if (payloadDecoder == null) {
                initPayloadDecoder(cipherKind, header.streamType(), in, out);
                if (payloadDecoder == null) {
                    return;
                }
            }
            payloadDecoder.decodePayload(in, out);
        }
    }

    private void decodePacket(StreamType streamType, boolean isAead2022, ByteBuf in, List<Object> out)
        throws InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (isAead2022) {
            int nonceLength = AEAD2022.UDP.getNonceLength(cipherKind);
            int tagSize = cipherMethod.tagSize();
            int headerLength = nonceLength + tagSize + 8 + 8 + 1 + 8 + 2;
            if (headerLength > in.readableBytes()) {
                String msg = String.format("packet too short, at least %d bytes, but found %d bytes", headerLength, in.readableBytes());
                throw new DecoderException(msg);
            }
            byte[] headerBytes = new byte[16];
            in.readBytes(headerBytes);
            ByteBuf header = Unpooled.wrappedBuffer(AES.ECB_NoPadding.decrypt(key, headerBytes));
            byte[] nonce = new byte[12];
            header.getBytes(4, nonce);
            long serverSessionId = header.readLong();
            header.readLong(); // packet_id
            List<Object> list = new ArrayList<>(1);
            AEAD2022.UDP.newPayloadDecoder(cipherMethod, key, serverSessionId, nonce).decodePacket(in, list);
            ByteBuf decoded = (ByteBuf) list.get(0);
            decoded.readByte(); // stream type
            validateTimestamp(decoded);
            int paddingLength = decoded.readUnsignedShort();
            if (paddingLength > 0) {
                decoded.skipBytes(paddingLength);
            }
            if (StreamType.Request == streamType) {
                decoded.readLong(); // client_session_id
            }
            Address.decode(decoded, out);
            out.add(decoded.slice());
        } else {
            byte[] salt = new byte[requestSalt.length];
            in.readBytes(salt);
            List<Object> list = new ArrayList<>(1);
            AEAD.UDP.newPayloadDecoder(cipherMethod, key, salt).decodePacket(in, list);
            ByteBuf decoded = (ByteBuf) list.get(0);
            Address.decode(decoded, out);
            out.add(decoded.slice());
        }
    }

    private void initPayloadDecoder(CipherKind kind, StreamType streamType, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        if (in.readableBytes() < requestSalt.length) {
            return;
        }
        in.markReaderIndex();
        in.readBytes(requestSalt);
        if (logger.isTraceEnabled()) {
            logger.trace("request salt {}", Base64.getEncoder().encodeToString(requestSalt));
        }
        if (kind.isAead2022()) {
            initAEAD2022PayloadDecoder(streamType, in, out, requestSalt);
        } else {
            initPayloadDecoder(streamType, in, out, requestSalt);
        }
    }

    private void initAEAD2022PayloadDecoder(StreamType streamType, ByteBuf in, List<Object> out, byte[] salt) throws InvalidCipherTextException {
        int tagSize = cipherMethod.tagSize();
        PayloadDecoder newPayloadDecoder = AEAD2022.TCP.newPayloadDecoder(cipherMethod, key, salt);
        int saltSize = StreamType.Response == streamType ? 0 : requestSalt.length;
        byte[] encryptedHeaderBytes = new byte[1 + 8 + saltSize + 2 + tagSize];
        in.readBytes(encryptedHeaderBytes);
        Authenticator auth = newPayloadDecoder.auth();
        ByteBuf headerBuf = Unpooled.wrappedBuffer(auth.open(encryptedHeaderBytes));
        byte streamTypeByte = headerBuf.readByte();
        StreamType expectedStreamType = switch (streamType) {
            case Request -> StreamType.Response;
            case Response -> StreamType.Request;
        };
        byte expectedStreamTypeByte = expectedStreamType.getValue();
        if (expectedStreamTypeByte != streamTypeByte) {
            String msg = String.format("invalid stream type, expecting %d, but found %d", expectedStreamTypeByte, streamTypeByte);
            throw new DecoderException(msg);
        }
        validateTimestamp(headerBuf);
        if (logger.isTraceEnabled()) {
            headerBuf.readBytes(requestSalt);
            logger.trace("request salt {}", Base64.getEncoder().encodeToString(requestSalt));
        } else {
            headerBuf.skipBytes(saltSize);
        }
        int length = headerBuf.readUnsignedShort();
        if (in.readableBytes() < length + tagSize) {
            in.resetReaderIndex();
            return;
        }
        byte[] encryptedPayloadBytes = new byte[length + tagSize];
        in.readBytes(encryptedPayloadBytes);
        ByteBuf first = Unpooled.wrappedBuffer(auth.open(encryptedPayloadBytes));
        if (StreamType.Response == streamType) {
            Address.decode(first, out);
            int paddingLength = first.readUnsignedShort();
            first.skipBytes(paddingLength);
            out.add(first);
        } else {
            out.add(first);
        }
        this.payloadDecoder = newPayloadDecoder;
    }

    private void initPayloadDecoder(StreamType streamType, ByteBuf in, List<Object> out, byte[] salt) throws InvalidCipherTextException {
        PayloadDecoder newPayloadDecoder = AEAD.TCP.newPayloadDecoder(cipherMethod, key, salt);
        List<Object> list = new ArrayList<>(1);
        newPayloadDecoder.decodePayload(in, list);
        if (list.isEmpty()) {
            in.resetReaderIndex();
            return;
        }
        if (StreamType.Response == streamType) {
            Address.decode((ByteBuf) list.get(0), out);
        }
        out.addAll(list);
        this.payloadDecoder = newPayloadDecoder;
    }

    private static void validateTimestamp(ByteBuf decoded) {
        long timestamp = decoded.readLong();
        long now = AEAD2022.now();
        long diff = timestamp - now;
        if (Math.abs(diff) > AEAD2022.SERVER_STREAM_TIMESTAMP_MAX_DIFF) {
            String msg = String.format("invalid timestamp %d - now %d = %d", timestamp, now, diff);
            throw new DecoderException(msg);
        }
    }
}