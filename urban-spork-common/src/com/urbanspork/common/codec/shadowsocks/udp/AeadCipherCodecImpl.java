package com.urbanspork.common.codec.shadowsocks.udp;

import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.shadowsocks.Keys;
import com.urbanspork.common.protocol.shadowsocks.aead.AEAD;
import com.urbanspork.common.protocol.socks.Address;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * AEAD Cipher Codec
 *
 * @author Zmax0
 * @see <a href=https://shadowsocks.org/doc/aead.html">AEAD ciphers</a>
 */
class AeadCipherCodecImpl implements AeadCipherCodec {

    private final Keys keys;
    private final CipherMethod cipherMethod;

    public AeadCipherCodecImpl(CipherMethod cipherMethod, Keys keys) {
        this.keys = keys;
        this.cipherMethod = cipherMethod;
    }

    @Override
    public void encode(Context context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        InetSocketAddress address = context.address();
        byte[] salt = context.control().salt();
        out.writeBytes(salt);
        ByteBuf temp = Unpooled.buffer(Address.getLength(address));
        Address.encode(address, temp);
        AEAD.UDP.newPayloadEncoder(cipherMethod, keys.encKey(), salt).encodePacket(Unpooled.wrappedBuffer(temp, msg), out);
        msg.skipBytes(msg.readableBytes());
    }

    @Override
    public void decode(Context context, ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        byte[] salt = context.control().salt();
        in.readBytes(salt);
        ByteBuf packet = AEAD.UDP.newPayloadDecoder(cipherMethod, keys.encKey(), salt).decodePacket(in);
        Address.decode(packet, out);
        out.add(packet.slice());
    }
}