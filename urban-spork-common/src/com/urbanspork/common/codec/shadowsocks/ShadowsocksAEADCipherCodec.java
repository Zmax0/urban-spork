package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;
import com.urbanspork.common.crypto.GeneralDigests;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.util.List;

import static java.lang.System.arraycopy;

/**
 * AEAD Cipher Codec
 *
 * @author Zmax0
 * @see <a href=https://shadowsocks.org/guide/aead.html">https://shadowsocks.org/guide/aead.html</a>
 */
class ShadowsocksAEADCipherCodec extends ByteToMessageCodec<ByteBuf> implements AEADPayloadEncoder, AEADPayloadDecoder {

    /*
     * TCP per-session [salt][encrypted payload length][length tag][encrypted payload][payload tag]
     * UDP per-packet [salt][encrypted payload][payload tag]
     */

    private static final int NONCE_SIZE = 12;
    private static final int PAYLOAD_LIMIT = 0xffff;
    private static final byte[] INFO = new byte[]{115, 115, 45, 115, 117, 98, 107, 101, 121}; // "ss-subkey"

    private final byte[] nonce = new byte[NONCE_SIZE];
    private final byte[] key;
    private final int saltSize;
    private final AEADCipherCodec codec;
    private final ChunkSizeCodec chunkSizeCodec;
    private final Network network;
    private int payloadLength = INIT_PAYLOAD_LENGTH;
    private AEADAuthenticator payloadEncoder;
    private AEADAuthenticator payloadDecoder;

    ShadowsocksAEADCipherCodec(String password, int saltSize, AEADCipherCodec codec, Network network) {
        this.key = generateKey(password.getBytes(), saltSize);
        this.saltSize = saltSize;
        this.codec = codec;
        this.chunkSizeCodec = Network.UDP != network ? generateChunkSizeCodec() : null;
        this.network = network;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (network == Network.UDP) {
            byte[] salt = Dice.randomBytes(saltSize);
            out.writeBytes(salt);
            payloadEncoder = newAuthenticator(salt);
            encodePacket(msg, out);
        } else {
            if (payloadEncoder == null) {
                byte[] salt = Dice.randomBytes(saltSize);
                out.writeBytes(salt);
                payloadEncoder = newAuthenticator(salt);
            }
            encodePayload(msg, out);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= saltSize) {
            if (network == Network.UDP) {
                byte[] salt = new byte[saltSize];
                in.readBytes(salt);
                payloadDecoder = newAuthenticator(salt);
                decodePacket(in, out);
            } else {
                if (payloadDecoder == null) {
                    byte[] salt = new byte[saltSize];
                    in.readBytes(salt);
                    payloadDecoder = newAuthenticator(salt);
                }
                decodePayload(in, out);
            }
        }
    }

    @Override
    public ChunkSizeCodec chunkSizeEncoder() {
        return chunkSizeCodec;
    }

    @Override
    public ChunkSizeCodec chunkSizeDecoder() {
        return chunkSizeCodec;
    }

    @Override
    public AEADAuthenticator payloadEncoder() {
        return payloadEncoder;
    }

    @Override
    public AEADAuthenticator payloadDecoder() {
        return payloadDecoder;
    }

    @Override
    public int payloadLimit() {
        return PAYLOAD_LIMIT;
    }

    @Override
    public int payloadLength() {
        return payloadLength;
    }

    @Override
    public void updatePayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    // ensure key.length equals salt.length
    private byte[] generateKey(byte[] password, int size) {
        byte[] encoded = new byte[size];
        byte[] passwordDigest = GeneralDigests.md5.get(password);
        byte[] container = new byte[password.length + passwordDigest.length];
        arraycopy(passwordDigest, 0, encoded, 0, Math.min(size, passwordDigest.length));
        int index = passwordDigest.length;
        while (index < size) {
            arraycopy(passwordDigest, 0, container, 0, passwordDigest.length);
            arraycopy(password, 0, container, passwordDigest.length, password.length);
            passwordDigest = GeneralDigests.md5.get(container);
            arraycopy(passwordDigest, 0, encoded, index, Math.min(size - index, passwordDigest.length));
            index += passwordDigest.length;
        }
        return encoded;
    }

    private AEADAuthenticator newAuthenticator(byte[] salt) {
        return new AEADAuthenticator(codec, hkdfsha1(key, salt),
            NonceGenerator.generateCountingNonce(nonce, codec.nonceSize(), false),
            BytesGenerator.generateEmptyBytes());
    }

    private byte[] hkdfsha1(byte[] key, byte[] salt) {
        byte[] out = new byte[salt.length];
        HKDFBytesGenerator generator = new HKDFBytesGenerator(new SHA1Digest());
        generator.init(new HKDFParameters(key, salt, INFO));
        generator.generateBytes(out, 0, out.length);
        return out;
    }

    private ChunkSizeCodec generateChunkSizeCodec() {
        return new ChunkSizeCodec() {
            @Override
            public byte[] encode(int size) throws InvalidCipherTextException {
                byte[] bytes = new byte[chunkSizeCodec.sizeBytes()];
                Unpooled.wrappedBuffer(bytes).setShort(0, Math.min(size, PAYLOAD_LIMIT));
                return payloadEncoder.seal(bytes);
            }

            @Override
            public int decode(byte[] data) throws InvalidCipherTextException {
                return Unpooled.wrappedBuffer(payloadDecoder.open(data)).getUnsignedShort(0);
            }
        };
    }
}