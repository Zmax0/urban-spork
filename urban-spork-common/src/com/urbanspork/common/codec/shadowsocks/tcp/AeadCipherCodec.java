package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.Authenticator;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.codec.shadowsocks.Keys;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.protocol.shadowsocks.aead.AEAD;
import com.urbanspork.common.protocol.shadowsocks.aead2022.AEAD2022;
import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.transport.tcp.RelayingPayload;
import com.urbanspork.common.util.ByteString;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * AEAD Cipher Codec
 *
 * @author Zmax0
 * @see <a href=https://shadowsocks.org/doc/aead.html">AEAD ciphers</a>
 */
class AeadCipherCodec {

    private static final Logger logger = LoggerFactory.getLogger(AeadCipherCodec.class);
    private final Keys keys;
    private final CipherKind cipherKind;
    private final CipherMethod cipherMethod;
    private PayloadEncoder payloadEncoder;
    private PayloadDecoder payloadDecoder;

    AeadCipherCodec(CipherKind cipherKind, CipherMethod cipherMethod, Keys keys) {
        this.keys = keys;
        this.cipherKind = cipherKind;
        this.cipherMethod = cipherMethod;
    }

    public void encode(Session session, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        boolean isAead2022 = cipherKind.isAead2022();
        if (payloadEncoder == null) {
            initPayloadEncoder(session, isAead2022, out);
            logger.trace("[tcp][encode identity]{}", session.identity());
            if (Mode.Client == session.mode()) {
                msg = handleClientHeader(session, isAead2022, msg, out);
            } else {
                handleServerHeader(session, isAead2022, msg, out);
            }
        }
        payloadEncoder.encodePayload(msg, out);
    }

    private void initPayloadEncoder(Session session, boolean isAead2022, ByteBuf out) {
        withIdentity(session, cipherKind, keys, out);
        byte[] salt = session.identity().salt();
        if (isAead2022) {
            ServerUser user = session.identity().getUser();
            if (user != null) {
                payloadEncoder = AEAD2022.TCP.newPayloadEncoder(cipherMethod, user.key(), salt);
            } else {
                payloadEncoder = AEAD2022.TCP.newPayloadEncoder(cipherMethod, keys.encKey(), salt);
            }
        } else {
            payloadEncoder = AEAD.TCP.newPayloadEncoder(cipherMethod, keys.encKey(), salt);
        }
    }

    private ByteBuf handleClientHeader(Session session, boolean isAead2022, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        ByteBuf temp = Unpooled.buffer();
        Address.encode(session.request(), temp);
        if (isAead2022) {
            int paddingLength = AEAD2022.getPaddingLength(msg);
            temp.writeShort(paddingLength);
            temp.writeBytes(Dice.rollBytes(paddingLength));
        }
        temp = Unpooled.wrappedBuffer(temp, msg);
        msg.skipBytes(msg.readableBytes());
        if (isAead2022) {
            for (byte[] bytes : AEAD2022.TCP.newHeader(session.mode(), session.identity().getRequestSalt(), temp)) {
                out.writeBytes(payloadEncoder.auth().seal(bytes));
            }
        }
        return temp;
    }

    private void handleServerHeader(Session session, boolean isAead2022, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        if (isAead2022) {
            for (byte[] bytes : AEAD2022.TCP.newHeader(session.mode(), session.identity().getRequestSalt(), msg)) {
                out.writeBytes(payloadEncoder.auth().seal(bytes));
            }
        }
    }

    public void decode(Session session, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        if (payloadDecoder == null) {
            initPayloadDecoder(session, cipherKind, in, out);
            if (payloadDecoder == null) {
                return;
            }
            logger.trace("[tcp][decode identity]{}", session.identity());
        }
        payloadDecoder.decodePayload(in, out);
    }

    private void initPayloadDecoder(Session session, CipherKind kind, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        if (in.readableBytes() < session.identity().salt().length) {
            return;
        }
        in.markReaderIndex();
        if (kind.isAead2022()) {
            initAEAD2022PayloadDecoder(session, in, out);
        } else {
            initPayloadDecoder(session, in, out);
        }
    }

    private void initAEAD2022PayloadDecoder(Session session, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        int tagSize = cipherMethod.tagSize();
        int saltLength = cipherKind.keySize();
        int requestSaltLength = Mode.Server == session.mode() ? 0 : saltLength;
        boolean requireEih = Mode.Server == session.mode() && cipherKind.supportEih() && session.userManager().userCount() > 0;
        int eihLength = requireEih ? 16 : 0;
        byte[] salt = new byte[saltLength];
        in.readBytes(salt);
        if (logger.isTraceEnabled()) {
            logger.trace("get AEAD salt {}", ByteString.valueOf(salt));
        }
        if (session.context().checkNonceReplay(salt)) {
            String msg = String.format("detected repeated nonce salt %s", ByteString.valueOf(salt));
            throw new RepeatedNonceException(msg);
        }
        session.identity().setRequestSalt(salt);
        ByteBuf sealedHeaderBuf = Unpooled.buffer();
        int sealedHeaderLength = eihLength + 1 + 8 + requestSaltLength + 2 + tagSize;
        if (in.readableBytes() < sealedHeaderLength) {
            String msg = String.format("header too short, expecting %d bytes, but found %d bytes", sealedHeaderLength + saltLength, in.readableBytes() + saltLength);
            throw new TooShortHeaderException(msg);
        }
        in.getBytes(in.readerIndex(), sealedHeaderBuf, sealedHeaderLength);
        PayloadDecoder newPayloadDecoder;
        if (requireEih) {
            byte[] eih = new byte[16];
            sealedHeaderBuf.readBytes(eih);
            newPayloadDecoder = AEAD2022.TCP.newPayloadDecoder(cipherMethod, session.identity(), session.userManager(), keys.encKey(), salt, eih);
        } else {
            newPayloadDecoder = AEAD2022.TCP.newPayloadDecoder(cipherMethod, keys.encKey(), salt);
        }
        Authenticator auth = newPayloadDecoder.auth();
        byte[] sealedHeaderBytes = new byte[sealedHeaderBuf.readableBytes()];
        sealedHeaderBuf.readBytes(sealedHeaderBytes);
        ByteBuf headerBuf = Unpooled.wrappedBuffer(auth.open(sealedHeaderBytes));
        byte streamTypeByte = headerBuf.readByte();
        Mode expectedMode = switch (session.mode()) {
            case Client -> Mode.Server;
            case Server -> Mode.Client;
        };
        byte expectedStreamTypeByte = expectedMode.getValue();
        if (expectedStreamTypeByte != streamTypeByte) {
            String msg = String.format("invalid stream type, expecting %d, but found %d", expectedStreamTypeByte, streamTypeByte);
            throw new DecoderException(msg);
        }
        AEAD2022.validateTimestamp(headerBuf.readLong());
        if (Mode.Client == session.mode()) {
            byte[] requestSalt = new byte[salt.length];
            headerBuf.readBytes(requestSalt);
            session.identity().setRequestSalt(requestSalt);
        }
        in.skipBytes(sealedHeaderLength);
        int length = headerBuf.readUnsignedShort();
        if (in.readableBytes() < length + tagSize) {
            session.context().resetNonceReplay(salt);
            in.resetReaderIndex();
            return;
        }
        byte[] encryptedPayloadBytes = new byte[length + tagSize];
        in.readBytes(encryptedPayloadBytes);
        ByteBuf first = Unpooled.wrappedBuffer(auth.open(encryptedPayloadBytes));
        if (Mode.Server == session.mode()) {
            InetSocketAddress address = Address.decode(first);
            int paddingLength = first.readUnsignedShort();
            first.skipBytes(paddingLength);
            out.add(new RelayingPayload<>(address, first));
        } else {
            out.add(first);
        }
        this.payloadDecoder = newPayloadDecoder;
    }

    private void initPayloadDecoder(Session session, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        byte[] salt = session.identity().salt();
        in.readBytes(salt);
        PayloadDecoder newPayloadDecoder = AEAD.TCP.newPayloadDecoder(cipherMethod, keys.encKey(), salt);
        List<Object> list = new ArrayList<>();
        newPayloadDecoder.decodePayload(in, list);
        if (list.isEmpty()) {
            in.resetReaderIndex();
            return;
        }
        if (Mode.Server == session.mode()) {
            ByteBuf first = (ByteBuf) list.getFirst();
            InetSocketAddress address = Address.decode(first);
            out.add(new RelayingPayload<>(address, Unpooled.EMPTY_BUFFER));
        }
        out.addAll(list);
        this.payloadDecoder = newPayloadDecoder;
    }

    static void withIdentity(Session session, CipherKind kind, Keys keys, ByteBuf out) {
        byte[] salt = session.identity().salt();
        out.writeBytes(salt); // salt should be sent with the first chunk
        if (Mode.Client == session.mode() && kind.supportEih()) {
            AEAD2022.TCP.withEih(kind, keys, salt, out);
        }
    }
}