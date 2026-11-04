package com.urbanspork.common.codec.chunk;

public interface ChunkSizeCodec extends AutoCloseable {

    int sizeBytes();

    byte[] encode(int size);

    int decode(byte[] data);

    default void close() throws Exception {

    }
}
