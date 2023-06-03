package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

public record VMessAEADChunkSizeCodec(AEADAuthenticator auth) implements ChunkSizeCodec {

    public static final byte[] AUTH_LEN = "auth_len".getBytes();

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
