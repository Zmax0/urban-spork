package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

public class ClientAEADChunkSizeCodec implements ChunkSizeCodec {

    private static final byte[] AUTH_LEN = "auth_len".getBytes();

    private final AEADAuthenticator auth;

    public ClientAEADChunkSizeCodec(AEADCipherCodec codec, ClientSession session) {
        this(codec, session, NonceGenerator.generateCountingNonce(session.requestBodyIV, codec.nonceSize()));
    }

    public ClientAEADChunkSizeCodec(AEADCipherCodec codec, ClientSession session, NonceGenerator nonceGenerator) {
        auth = new AEADAuthenticator(codec, KDF.kdf16(session.requestBodyKey, AUTH_LEN), nonceGenerator, BytesGenerator.generateEmptyBytes());
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
