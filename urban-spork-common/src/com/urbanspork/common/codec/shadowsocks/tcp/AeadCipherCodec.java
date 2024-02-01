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
import com.urbanspork.common.util.ByteString;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public void encode(Context context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        boolean isAead2022 = cipherKind.isAead2022();
        if (payloadEncoder == null) {
            initTcpPayloadEncoder(context, isAead2022, out);
            logger.trace("[tcp][encode session]{}", context.session());
            if (Mode.Client == context.mode()) {
                msg = handleRequestHeader(context, isAead2022, msg, out);
            } else {
                handleResponseHeader(context, isAead2022, msg, out);
            }
        }
        payloadEncoder.encodePayload(msg, out);
    }

    private void initTcpPayloadEncoder(Context context, boolean isAead2022, ByteBuf out) {
        withIdentity(context, cipherKind, keys, out);
        byte[] salt = context.session().salt();
        if (isAead2022) {
            ServerUser user = context.session().getUser();
            if (user != null) {
                payloadEncoder = AEAD2022.TCP.newPayloadEncoder(cipherMethod, user.key(), salt);
            } else {
                payloadEncoder = AEAD2022.TCP.newPayloadEncoder(cipherMethod, keys.encKey(), salt);
            }
        } else {
            payloadEncoder = AEAD.TCP.newPayloadEncoder(cipherMethod, keys.encKey(), salt);
        }
    }

    private ByteBuf handleRequestHeader(Context context, boolean isAead2022, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        ByteBuf temp = Unpooled.buffer();
        Address.encode(context.request(), temp);
        if (isAead2022) {
            int paddingLength = AEAD2022.getPaddingLength(msg);
            temp.writeShort(paddingLength);
            temp.writeBytes(Dice.rollBytes(paddingLength));
        }
        temp = Unpooled.wrappedBuffer(temp, msg);
        msg.skipBytes(msg.readableBytes());
        if (isAead2022) {
            for (byte[] bytes : AEAD2022.TCP.newHeader(context.mode(), context.session().getRequestSalt(), temp)) {
                out.writeBytes(payloadEncoder.auth().seal(bytes));
            }
        }
        return temp;
    }

    private void handleResponseHeader(Context context, boolean isAead2022, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        if (isAead2022) {
            for (byte[] bytes : AEAD2022.TCP.newHeader(context.mode(), context.session().getRequestSalt(), msg)) {
                out.writeBytes(payloadEncoder.auth().seal(bytes));
            }
        }
    }

    public void decode(Context context, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        if (payloadDecoder == null) {
            initPayloadDecoder(context, cipherKind, in, out);
            if (payloadDecoder == null) {
                return;
            }
            logger.trace("[tcp][decode session]{}", context.session());
        }
        payloadDecoder.decodePayload(in, out);
    }

    private void initPayloadDecoder(Context context, CipherKind kind, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        if (in.readableBytes() < context.session().salt().length) {
            return;
        }
        in.markReaderIndex();
        if (kind.isAead2022()) {
            initAEAD2022PayloadDecoder(context, in, out);
        } else {
            initPayloadDecoder(context, in, out);
        }
    }

    private void initAEAD2022PayloadDecoder(Context context, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        int tagSize = cipherMethod.tagSize();
        int saltLength = cipherKind.keySize();
        int requestSaltLength = Mode.Server == context.mode() ? 0 : saltLength;
        boolean requireEih = Mode.Server == context.mode() && cipherKind.supportEih() && context.userManager().userCount() > 0;
        int eihLength = requireEih ? 16 : 0;
        byte[] salt = new byte[saltLength];
        in.readBytes(salt);
        if (logger.isTraceEnabled()) {
            logger.trace("get AEAD salt {}", ByteString.valueOf(salt));
        }
        context.session().setRequestSalt(salt);
        ByteBuf sealedHeaderBuf = in.readBytes(eihLength + 1 + 8 + requestSaltLength + 2 + tagSize);
        PayloadDecoder newPayloadDecoder;
        if (requireEih) {
            if (sealedHeaderBuf.readableBytes() < 16) {
                String msg = String.format("expecting EIH, but header chunk len: %d", sealedHeaderBuf.readableBytes());
                throw new DecoderException(msg);
            }
            byte[] eih = new byte[16];
            sealedHeaderBuf.readBytes(eih);
            newPayloadDecoder = AEAD2022.TCP.newPayloadDecoder(cipherMethod, context.session(), context.userManager(), keys.encKey(), salt, eih);
        } else {
            newPayloadDecoder = AEAD2022.TCP.newPayloadDecoder(cipherMethod, keys.encKey(), salt);
        }
        Authenticator auth = newPayloadDecoder.auth();
        byte[] sealedHeaderBytes = new byte[sealedHeaderBuf.readableBytes()];
        sealedHeaderBuf.readBytes(sealedHeaderBytes);
        sealedHeaderBuf.release();
        ByteBuf headerBuf = Unpooled.wrappedBuffer(auth.open(sealedHeaderBytes));
        byte streamTypeByte = headerBuf.readByte();
        Mode expectedMode = switch (context.mode()) {
            case Client -> Mode.Server;
            case Server -> Mode.Client;
        };
        byte expectedStreamTypeByte = expectedMode.getValue();
        if (expectedStreamTypeByte != streamTypeByte) {
            String msg = String.format("invalid stream type, expecting %d, but found %d", expectedStreamTypeByte, streamTypeByte);
            throw new DecoderException(msg);
        }
        AEAD2022.validateTimestamp(headerBuf.readLong());
        if (Mode.Client == context.mode()) {
            byte[] requestSalt = new byte[salt.length];
            headerBuf.readBytes(requestSalt);
            context.session().setRequestSalt(requestSalt);
        }
        int length = headerBuf.readUnsignedShort();
        if (in.readableBytes() < length + tagSize) {
            in.resetReaderIndex();
            return;
        }
        byte[] encryptedPayloadBytes = new byte[length + tagSize];
        in.readBytes(encryptedPayloadBytes);
        ByteBuf first = Unpooled.wrappedBuffer(auth.open(encryptedPayloadBytes));
        if (Mode.Server == context.mode()) {
            Address.decode(first, out);
            int paddingLength = first.readUnsignedShort();
            first.skipBytes(paddingLength);
            out.add(first);
        } else {
            out.add(first);
        }
        this.payloadDecoder = newPayloadDecoder;
    }

    private void initPayloadDecoder(Context context, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        byte[] salt = context.session().salt();
        in.readBytes(salt);
        PayloadDecoder newPayloadDecoder = AEAD.TCP.newPayloadDecoder(cipherMethod, keys.encKey(), salt);
        List<Object> list = new ArrayList<>(1);
        newPayloadDecoder.decodePayload(in, list);
        if (list.isEmpty()) {
            in.resetReaderIndex();
            return;
        }
        if (Mode.Server == context.mode()) {
            Address.decode((ByteBuf) list.getFirst(), out);
        }
        out.addAll(list);
        this.payloadDecoder = newPayloadDecoder;
    }

    static void withIdentity(Context context, CipherKind kind, Keys keys, ByteBuf out) {
        byte[] salt = context.session().salt();
        out.writeBytes(salt); // salt should be sent with the first chunk
        if (Mode.Client == context.mode() && kind.supportEih()) {
            AEAD2022.TCP.withEih(keys, salt, out);
        }
    }
}