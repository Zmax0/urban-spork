package com.urbanspork.common.codec.chunk;

import org.bouncycastle.crypto.InvalidCipherTextException;

public enum EmptyChunkSizeParser implements ChunkSizeCodec {

    INSTANCE;

    @Override
    public int sizeBytes() {
        return 0;
    }

    @Override
    public byte[] encode(int size) throws InvalidCipherTextException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int decode(byte[] data) throws InvalidCipherTextException {
        throw new UnsupportedOperationException();
    }
}
