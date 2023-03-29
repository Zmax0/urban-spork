package com.urbanspork.common.codec.shadowsocks.base;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.CipherCodec;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.util.List;

/**
 * AEAD Cipher Codec
 *
 * @author Zmax0
 * @see <a href=https://shadowsocks.org/guide/aead.html">https://shadowsocks.org/guide/aead.html</a>
 */
public class ShadowsocksAEADCipherCodec implements AEADPayloadEncoder, AEADPayloadDecoder {

    /*
     * [encrypted payload length][length tag][encrypted payload][payload tag]
     */

    private static final int NONCE_SIZE = 12;
    private static final int PAYLOAD_SIZE = 65535;
    private static final byte[] INFO = new byte[]{115, 115, 45, 115, 117, 98, 107, 101, 121};
    private final byte[] nonce = new byte[NONCE_SIZE];
    private final int saltSize;
    private final byte[] key;
    private final AEADCipherCodec cipherCodec;

    private int payloadLength = INIT_PAYLOAD_LENGTH;
    private ChunkSizeCodec chunkSizeCodec;
    private AEADAuthenticator authenticator;

    public ShadowsocksAEADCipherCodec(AEADCipherCodec cipherCodec, byte[] key, int saltSize) {
        this.key = key;
        this.saltSize = saltSize;
        this.cipherCodec = cipherCodec;
    }

    public void encode(ByteBuf in, ByteBuf out) throws Exception {
        if (authenticator == null) {
            byte[] salt = CipherCodec.randomBytes(saltSize);
            out.writeBytes(salt);
            authenticator = new AEADAuthenticator(cipherCodec, generateKey(key, salt), nonce);
            chunkSizeCodec = generateChunkSizeCodec();
        }
        encodePayload(in, out);
    }

    public void decrypt(ByteBuf in, List<Object> out) throws Exception {
        if (authenticator == null && in.readableBytes() >= saltSize) {
            byte[] salt = new byte[saltSize];
            in.readBytes(salt, 0, saltSize);
            authenticator = new AEADAuthenticator(cipherCodec, generateKey(key, salt), nonce);
            chunkSizeCodec = generateChunkSizeCodec();
        }
        if (authenticator != null) {
            decodePayload(in, out);
        }
    }

    private byte[] generateKey(byte[] key, byte[] salt) {
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
                return authenticator.seal(bytes);
            }

            @Override
            public int decode(byte[] data) throws Exception {
                return Unpooled.wrappedBuffer(authenticator.open(data)).getUnsignedShort(0);
            }
        };
    }

    @Override
    public ChunkSizeCodec chunkSizeCodec() {
        return chunkSizeCodec;
    }

    @Override
    public AEADAuthenticator authenticator() {
        return authenticator;
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
}