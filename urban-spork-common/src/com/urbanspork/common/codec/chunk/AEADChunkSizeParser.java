package com.urbanspork.common.codec.chunk;

import com.urbanspork.common.codec.aead.Authenticator;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

public record AEADChunkSizeParser(Authenticator auth) implements ChunkSizeCodec {

    @Override
    public int sizeBytes() {
        return Short.BYTES + auth().overhead();
    }

    @Override
    public byte[] encode(int size) throws InvalidCipherTextException {
        byte[] out = new byte[Short.BYTES];
        Unpooled.wrappedBuffer(out).setShort(0, size - auth().overhead());
        return auth.seal(out);
    }

    @Override
    public int decode(byte[] data) throws InvalidCipherTextException {
        return Unpooled.wrappedBuffer(auth.open(data)).readUnsignedShort() + auth().overhead();
    }
}
