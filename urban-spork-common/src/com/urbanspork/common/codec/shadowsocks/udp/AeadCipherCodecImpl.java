package com.urbanspork.common.codec.shadowsocks.udp;

import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.shadowsocks.Keys;
import com.urbanspork.common.protocol.shadowsocks.aead.AEAD;
import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.transport.udp.RelayingPacket;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.net.InetSocketAddress;

/**
 * AEAD Cipher Codec
 *
 * @author Zmax0
 * @see <a href=https://shadowsocks.org/doc/aead.html">AEAD ciphers</a>
 */
record AeadCipherCodecImpl(CipherMethod cipherMethod, Keys keys) implements AeadCipherCodec {

    @Override
    public void encode(Context context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        InetSocketAddress address = context.address();
        byte[] salt = Dice.rollBytes(cipherMethod.keySize());
        out.writeBytes(salt);
        ByteBuf temp = Unpooled.buffer(Address.getLength(address));
        Address.encode(address, temp);
        AEAD.UDP.newPayloadEncoder(cipherMethod, keys.encKey(), salt).encodePacket(Unpooled.wrappedBuffer(temp, msg), out);
        msg.skipBytes(msg.readableBytes());
    }

    @Override
    public RelayingPacket<ByteBuf> decode(Context context, ByteBuf in) throws InvalidCipherTextException {
        byte[] salt = new byte[cipherMethod.keySize()];
        in.readBytes(salt);
        ByteBuf packet = AEAD.UDP.newPayloadDecoder(cipherMethod, keys.encKey(), salt).decodePacket(in);
        InetSocketAddress address = Address.decode(packet);
        return new RelayingPacket<>(address, packet);
    }
}