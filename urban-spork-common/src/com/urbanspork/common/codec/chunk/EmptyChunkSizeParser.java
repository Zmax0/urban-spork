package com.urbanspork.common.codec.chunk;

public enum EmptyChunkSizeParser implements ChunkSizeCodec {

    INSTANCE;

    @Override
    public int sizeBytes() {
        return 0;
    }

    @Override
    public byte[] encode(int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int decode(byte[] data) {
        throw new UnsupportedOperationException();
    }
}
