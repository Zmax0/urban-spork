package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

class ClientAEADChunkSizeCodec implements ChunkSizeCodec {

    static final byte[] AUTH_LEN = "auth_len".getBytes();

    private final AEADAuthenticator auth;

    ClientAEADChunkSizeCodec(AEADCipherCodec codec, byte[] key, byte[] nonce) {
        this(codec, key, NonceGenerator.generateCountingNonce(nonce, codec.nonceSize()));
    }

    ClientAEADChunkSizeCodec(AEADCipherCodec codec, byte[] key, NonceGenerator nonceGenerator) {
        auth = new AEADAuthenticator(codec, key, nonceGenerator, BytesGenerator.generateEmptyBytes());
    }

    @Override
    public byte[] encode(int size) throws InvalidCipherTextException {
        byte[] out = new byte[sizeBytes()];
        Unpooled.wrappedBuffer(out).setShort(0, size);
        return auth.seal(out);
    }

    @Override
    public int decode(byte[] data) throws InvalidCipherTextException {
        return Unpooled.wrappedBuffer(auth.open(data)).readUnsignedShort();
    }

}
