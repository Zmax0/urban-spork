package com.urbanspork.common.codec.chunk;

import org.bouncycastle.crypto.InvalidCipherTextException;

public interface ChunkSizeCodec extends AutoCloseable {

    int sizeBytes();

    byte[] encode(int size) throws InvalidCipherTextException;

    int decode(byte[] data) throws InvalidCipherTextException;

    default void close() throws Exception {

    }
}
