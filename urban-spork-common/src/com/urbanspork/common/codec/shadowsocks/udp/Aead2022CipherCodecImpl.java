package com.urbanspork.common.codec.shadowsocks.udp;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.shadowsocks.Keys;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.protocol.shadowsocks.Control;
import com.urbanspork.common.protocol.shadowsocks.aead2022.AEAD2022;
import com.urbanspork.common.protocol.shadowsocks.aead2022.UdpCipher;
import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.transport.udp.RelayingPacket;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

class Aead2022CipherCodecImpl implements AeadCipherCodec {
    private static final Logger logger = LoggerFactory.getLogger(Aead2022CipherCodecImpl.class);
    private final Keys keys;
    private final CipherKind cipherKind;
    private final CipherMethod cipherMethod;

    Aead2022CipherCodecImpl(CipherKind cipherKind, CipherMethod cipherMethod, Keys keys) {
        this.keys = keys;
        this.cipherKind = cipherKind;
        this.cipherMethod = cipherMethod;
    }

    @Override
    public void encode(Context context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        if (Mode.Client == context.mode()) {
            encodeClientPacketAead2022(context, msg, out);
        } else {
            encodeServerPacketAead2022(context, msg, out);
        }
    }

    // Client -> Server
    private void encodeClientPacketAead2022(Context context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        Control control = context.control();
        InetSocketAddress address = context.address();
        int paddingLength = AEAD2022.getPaddingLength(msg);
        int nonceLength = AEAD2022.UDP.getNonceLength(cipherKind);
        int tagSize = cipherMethod.tagSize();
        byte[][] identityKeys = keys.identityKeys();
        boolean requireEih = cipherKind.supportEih() && identityKeys.length > 0;
        int eihSize = requireEih ? identityKeys.length * 16 : 0;
        ByteBuf temp = Unpooled.buffer(nonceLength + 8 + 8 + eihSize + 1 + 8 + 2 + paddingLength + Address.getLength(address) + msg.readableBytes() + tagSize);
        // header fields
        temp.writeLong(control.getClientSessionId());
        temp.writeLong(control.getPacketId());
        if (requireEih) {
            byte[] sessionIdPacketId = new byte[16];
            temp.getBytes(nonceLength, sessionIdPacketId);
            AEAD2022.UDP.withEih(cipherKind, keys.encKey(), identityKeys, sessionIdPacketId, temp);
        }
        temp.writeByte(Mode.Client.getValue());
        temp.writeLong(AEAD2022.newTimestamp());
        temp.writeShort(paddingLength);
        temp.writeBytes(Dice.rollBytes(paddingLength));
        Address.encode(address, temp);
        temp.writeBytes(msg);
        UdpCipher cipher = AEAD2022.UDP.getCipher(cipherKind, cipherMethod, keys.encKey(), control.getClientSessionId());
        byte[] iPSK = identityKeys.length == 0 ? keys.encKey() : identityKeys[0];
        AEAD2022.UDP.encodePacket(cipher, iPSK, eihSize, temp, out);
    }

    // Server -> Client
    private void encodeServerPacketAead2022(Context context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        Control control = context.control();
        int paddingLength = AEAD2022.getPaddingLength(msg);
        int nonceLength = AEAD2022.UDP.getNonceLength(cipherKind);
        InetSocketAddress address = context.address();
        ByteBuf temp = Unpooled.buffer(nonceLength + 8 + 8 + 1 + 8 + 8 + 2 + paddingLength + Address.getLength(address) + msg.readableBytes() + cipherMethod.tagSize());
        // header fields
        temp.writeLong(control.getServerSessionId());
        temp.writeLong(control.getPacketId());
        temp.writeByte(Mode.Server.getValue());
        temp.writeLong(AEAD2022.newTimestamp());
        temp.writeLong(control.getClientSessionId());
        temp.writeShort(paddingLength);
        temp.writeBytes(Dice.rollBytes(paddingLength));
        Address.encode(address, temp);
        temp.writeBytes(msg);
        ServerUser user = control.getUser();
        byte[] key;
        if (user != null) {
            key = user.key();
            logger.trace("udp encrypt with {} identity", user);
        } else {
            key = keys.encKey();
        }
        UdpCipher cipher = AEAD2022.UDP.getCipher(cipherKind, cipherMethod, key, control.getServerSessionId());
        AEAD2022.UDP.encodePacket(cipher, key, 0, temp, out);
    }

    @Override
    public RelayingPacket<ByteBuf> decode(Context context, ByteBuf in) throws InvalidCipherTextException {
        if (Mode.Client == context.mode()) {
            return decodeServerPocketAead2022(context, in);
        } else {
            return decodeClientPocketAead2022(context, in);
        }
    }

    // Client -> Server(*)
    private RelayingPacket<ByteBuf> decodeClientPocketAead2022(Context context, ByteBuf in) throws InvalidCipherTextException {
        int nonceLength = AEAD2022.UDP.getNonceLength(cipherKind);
        int tagSize = cipherMethod.tagSize();
        boolean requireEih = cipherKind.supportEih() && context.userManager().userCount() > 0;
        int eihSize = requireEih ? 16 : 0;
        int headerLength = nonceLength + tagSize + 8 + 8 + eihSize + 1 + 8 + 2;
        if (headerLength > in.readableBytes()) {
            String msg = String.format("packet too short, at least %d bytes, but found %d bytes", headerLength, in.readableBytes());
            throw new DecoderException(msg);
        }
        ByteBuf packet = AEAD2022.UDP.decodePacket(cipherKind, cipherMethod, context.control(), context.userManager(), keys.encKey(), in);
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
        control.setPacketId(packetId);
        InetSocketAddress address = Address.decode(packet);
        return new RelayingPacket<>(address, packet);
    }

    // Server -> Client(*)
    private RelayingPacket<ByteBuf> decodeServerPocketAead2022(Context context, ByteBuf in) throws InvalidCipherTextException {
        int nonceLength = AEAD2022.UDP.getNonceLength(cipherKind);
        int tagSize = cipherMethod.tagSize();
        int headerLength = nonceLength + tagSize + 8 + 8 + 1 + 8 + 2;
        if (headerLength > in.readableBytes()) {
            String msg = String.format("packet too short, at least %d bytes, but found %d bytes", headerLength, in.readableBytes());
            throw new DecoderException(msg);
        }
        ByteBuf packet = AEAD2022.UDP.decodePacket(cipherKind, cipherMethod, context.control(), context.userManager(), keys.encKey(), in);
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
        InetSocketAddress address = Address.decode(packet);
        return new RelayingPacket<>(address, packet);
    }
}