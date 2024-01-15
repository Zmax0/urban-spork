package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.Authenticator;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.shadowsocks.aead.AEAD;
import com.urbanspork.common.protocol.shadowsocks.aead2022.AEAD2022;
import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.util.ByteString;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
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
class AEADCipherCodec {

    private static final Logger logger = LoggerFactory.getLogger(AEADCipherCodec.class);
    private final Keys keys;
    private final CipherKind cipherKind;
    private final CipherMethod cipherMethod;
    private PayloadEncoder payloadEncoder;
    private PayloadDecoder payloadDecoder;

    AEADCipherCodec(CipherKind cipherKind, CipherMethod cipherMethod, Keys keys) {
        this.keys = keys;
        this.cipherKind = cipherKind;
        this.cipherMethod = cipherMethod;
    }

    public void encode(Context context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        boolean isAead2022 = cipherKind.isAead2022();
        if (context.network() == Network.UDP) {
            encodePacket(context, isAead2022, msg, out);
        } else {
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
    }

    private void encodePacket(Context context, boolean isAead2022, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        if (isAead2022) {
            if (Mode.Client == context.mode()) {
                encodeClientPacketAead2022(context, msg, out);
            } else {
                encodeServerPacketAead2022(context, msg, out);
            }
        } else {
            Socks5CommandRequest request = context.request();
            byte[] salt = context.session().salt();
            out.writeBytes(salt);
            ByteBuf temp = Unpooled.buffer(Address.getLength(request));
            Address.encode(request, temp);
            AEAD.UDP.newPayloadEncoder(cipherMethod, keys.encKey(), salt).encodePacket(Unpooled.wrappedBuffer(temp, msg), out);
            msg.skipBytes(msg.readableBytes());
        }
    }

    // Client -> Server
    private void encodeClientPacketAead2022(Context context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        Session session = context.session();
        logger.trace("[udp][encode session]{}", session);
        Socks5CommandRequest request = context.request();
        int paddingLength = AEAD2022.getPaddingLength(msg);
        int nonceLength = AEAD2022.UDP.getNonceLength(cipherKind);
        int tagSize = cipherMethod.tagSize();
        byte[][] identityKeys = keys.identityKeys();
        boolean requireEih = cipherKind.supportEih() && identityKeys.length > 0;
        int eihSize = requireEih ? identityKeys.length * 16 : 0;
        ByteBuf temp = Unpooled.buffer(nonceLength + 8 + 8 + eihSize + 1 + 8 + 2 + paddingLength + Address.getLength(request) + msg.readableBytes() + tagSize);
        // header fields
        temp.writeLong(session.getClientSessionId());
        temp.writeLong(session.getPacketId());
        if (requireEih) {
            byte[] sessionIdPacketId = new byte[16];
            temp.getBytes(nonceLength, sessionIdPacketId);
            AEAD2022.UDP.withEih(keys.encKey(), identityKeys, sessionIdPacketId, temp);
        }
        temp.writeByte(Mode.Client.getValue());
        temp.writeLong(AEAD2022.newTimestamp());
        temp.writeShort(paddingLength);
        temp.writeBytes(Dice.rollBytes(paddingLength));
        Address.encode(request, temp);
        temp.writeBytes(msg);
        UdpCipher cipher = AEAD2022.UDP.getCipher(cipherKind, cipherMethod, keys.encKey(), session.getClientSessionId());
        byte[] iPSK = identityKeys.length == 0 ? keys.encKey() : identityKeys[0];
        AEAD2022.UDP.encodePacket(cipher, iPSK, eihSize, temp, out);
    }

    // Server -> Client
    private void encodeServerPacketAead2022(Context context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        Session session = context.session();
        Control control = context.control();
        logger.trace("[udp][encode control]{}", control);
        int paddingLength = AEAD2022.getPaddingLength(msg);
        int nonceLength = AEAD2022.UDP.getNonceLength(cipherKind);
        Socks5CommandRequest request = context.request();
        ByteBuf temp = Unpooled.buffer(nonceLength + 8 + 8 + 1 + 8 + 8 + 2 + paddingLength + Address.getLength(request) + msg.readableBytes() + cipherMethod.tagSize());
        // header fields
        temp.writeLong(session.getServerSessionId());
        temp.writeLong(control.getPacketId());
        temp.writeByte(Mode.Server.getValue());
        temp.writeLong(AEAD2022.newTimestamp());
        temp.writeLong(control.getClientSessionId());
        temp.writeShort(paddingLength);
        temp.writeBytes(Dice.rollBytes(paddingLength));
        Address.encode(request, temp);
        temp.writeBytes(msg);
        ServerUser user = context.session().getUser();
        byte[] key;
        if (user != null) {
            key = user.key();
            logger.trace("udp encrypt with {} identity", user);
        } else {
            key = keys.encKey();
        }
        UdpCipher cipher = AEAD2022.UDP.getCipher(cipherKind, cipherMethod, key, session.getServerSessionId());
        AEAD2022.UDP.encodePacket(cipher, key, 0, temp, out);
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
        if (context.network() == Network.UDP) {
            decodePacket(context, cipherKind.isAead2022(), in, out);
        } else {
            if (payloadDecoder == null) {
                initPayloadDecoder(context, cipherKind, in, out);
                if (payloadDecoder == null) {
                    return;
                }
                logger.trace("[tcp][decode session]{}", context.session());
            }
            payloadDecoder.decodePayload(in, out);
        }
    }

    private void decodePacket(Context context, boolean isAead2022, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        if (isAead2022) {
            if (Mode.Client == context.mode()) {
                decodeServerPocketAead2022(context, in, out);
            } else {
                decodeClientPocketAead2022(context, in, out);
            }
        } else {
            byte[] salt = context.session().salt();
            in.readBytes(salt);
            ByteBuf packet = AEAD.UDP.newPayloadDecoder(cipherMethod, keys.encKey(), salt).decodePacket(in);
            Address.decode(packet, out);
            out.add(packet.slice());
        }
    }

    // Client -> Server
    private void decodeClientPocketAead2022(Context context, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        int nonceLength = AEAD2022.UDP.getNonceLength(cipherKind);
        int tagSize = cipherMethod.tagSize();
        boolean requireEih = cipherKind.supportEih() && context.userManager().userCount() > 0;
        int eihSize = requireEih ? 16 : 0;
        int headerLength = nonceLength + tagSize + 8 + 8 + eihSize + 1 + 8 + 2;
        if (headerLength > in.readableBytes()) {
            String msg = String.format("packet too short, at least %d bytes, but found %d bytes", headerLength, in.readableBytes());
            throw new DecoderException(msg);
        }
        ByteBuf packet = AEAD2022.UDP.decodePacket(cipherKind, cipherMethod, context, keys.encKey(), in);
        long clientSessionId = packet.readLong();
        long packetId = packet.readLong();
        if (requireEih) {
            packet.skipBytes(16);
        }
        byte socketType = packet.readByte();
        if (Mode.Client.getValue() != socketType) {
            String msg = String.format("invalid socket type, expecting %d, but found %d", Mode.Client.getValue(), socketType);
            throw new DecoderException(msg);
        }
        AEAD2022.validateTimestamp(packet.readLong());
        int paddingLength = packet.readUnsignedShort();
        if (paddingLength > 0) {
            packet.skipBytes(paddingLength);
        }
        Control control = context.control();
        control.setClientSessionId(clientSessionId);
        control.setServerSessionId(0);
        control.setPacketId(packetId);
        logger.trace("[udp][decode control]{}", control);
        Address.decode(packet, out);
        out.add(packet.slice());
    }

    // Server -> Client
    private void decodeServerPocketAead2022(Context context, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        int nonceLength = AEAD2022.UDP.getNonceLength(cipherKind);
        int tagSize = cipherMethod.tagSize();
        int headerLength = nonceLength + tagSize + 8 + 8 + 1 + 8 + 2;
        if (headerLength > in.readableBytes()) {
            String msg = String.format("packet too short, at least %d bytes, but found %d bytes", headerLength, in.readableBytes());
            throw new DecoderException(msg);
        }
        ByteBuf packet = AEAD2022.UDP.decodePacket(cipherKind, cipherMethod, context, keys.encKey(), in);
        long serverSessionId = packet.readLong();
        long packetId = packet.readLong();
        byte socketType = packet.readByte();
        if (Mode.Server.getValue() != socketType) {
            String msg = String.format("invalid socket type, expecting %d, but found %d", Mode.Server.getValue(), socketType);
            throw new DecoderException(msg);
        }
        AEAD2022.validateTimestamp(packet.readLong());
        long clientSessionId = packet.readLong();
        int paddingLength = packet.readUnsignedShort();
        if (paddingLength > 0) {
            packet.skipBytes(paddingLength);
        }
        Control control = context.control();
        control.setClientSessionId(clientSessionId);
        control.setServerSessionId(serverSessionId);
        control.setPacketId(packetId);
        Address.decode(packet, out);
        out.add(packet.slice());
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
            newPayloadDecoder = AEAD2022.TCP.newPayloadDecoder(cipherMethod, context, keys.encKey(), salt, eih);
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