package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.CipherCodec;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.bouncycastle.crypto.digests.MD5Digest;
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
     * [encrypted payload length][length tag][encrypted payload][payload tag]
     */

    private static final int NONCE_SIZE = 12;
    private static final int PAYLOAD_SIZE = 0xffff;
    private static final byte[] INFO = new byte[]{115, 115, 45, 115, 117, 98, 107, 101, 121};
    private final byte[] nonce = new byte[NONCE_SIZE];
    private final ChunkSizeCodec chunkSizeCodec = generateChunkSizeCodec();

    private int payloadLength = INIT_PAYLOAD_LENGTH;
    private final int saltSize;
    private final byte[] key;
    private final AEADCipherCodec codec;
    private AEADAuthenticator payloadEncoder;
    private AEADAuthenticator payloadDecoder;

    ShadowsocksAEADCipherCodec(byte[] password, int saltSize, AEADCipherCodec codec) {
        this.key = generateKey(password, saltSize);
        this.saltSize = saltSize;
        this.codec = codec;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (payloadEncoder == null) {
            byte[] salt = CipherCodec.randomBytes(saltSize);
            out.writeBytes(salt);
            payloadEncoder = new AEADAuthenticator(codec, hkdf(key, salt), nonce);
        }
        encodePayload(msg, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (payloadDecoder == null && in.readableBytes() >= saltSize) {
            byte[] salt = new byte[saltSize];
            in.readBytes(salt, 0, saltSize);
            payloadDecoder = new AEADAuthenticator(codec, hkdf(key, salt), nonce);
        }
        if (payloadDecoder != null) {
            decodePayload(in, out);
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
    public int payloadLength() {
        return payloadLength;
    }

    @Override
    public void updatePayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    @Override
    public int maxPayloadLength() {
        return PAYLOAD_SIZE;
    }

    private byte[] generateKey(byte[] password, int size) {
        byte[] encoded = new byte[size];
        MD5Digest digest = new MD5Digest();
        byte[] passwordDigest = md5digest(digest, password);
        byte[] container = new byte[password.length + passwordDigest.length];
        arraycopy(passwordDigest, 0, encoded, 0, Math.min(size, passwordDigest.length));
        int index = passwordDigest.length;
        while (index < size) {
            arraycopy(passwordDigest, 0, container, 0, passwordDigest.length);
            arraycopy(password, 0, container, passwordDigest.length, password.length);
            passwordDigest = md5digest(digest, container);
            arraycopy(passwordDigest, 0, encoded, index, Math.min(size - index, passwordDigest.length));
            index += passwordDigest.length;
        }
        return encoded;
    }

    private byte[] md5digest(MD5Digest digest, byte[] in) {
        digest.reset();
        digest.update(in, 0, in.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

    private byte[] hkdf(byte[] key, byte[] salt) {
        byte[] out = new byte[salt.length];
        HKDFBytesGenerator generator = new HKDFBytesGenerator(new SHA1Digest());
        generator.init(new HKDFParameters(key, salt, INFO));
        generator.generateBytes(out, 0, out.length);
        return out;
    }

    private ChunkSizeCodec generateChunkSizeCodec() {
        return new ChunkSizeCodec() {
            @Override
            public byte[] encode(int size) throws Exception {
                byte[] bytes = new byte[chunkSizeCodec.sizeBytes()];
                Unpooled.wrappedBuffer(bytes).setShort(0, Math.min(size, PAYLOAD_SIZE));
                return payloadEncoder.seal(bytes);
            }

            @Override
            public int decode(byte[] data) throws Exception {
                return Unpooled.wrappedBuffer(payloadDecoder.open(data)).getUnsignedShort(0);
            }
        };
    }
}