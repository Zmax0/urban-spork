package com.urbanspork.common.codec;

import org.bouncycastle.crypto.InvalidCipherTextException;

public interface ChunkSizeCodec {

    default int sizeBytes() {
        return Short.BYTES;
    }

    byte[] encode(int size) throws InvalidCipherTextException;

    int decode(byte[] data) throws InvalidCipherTextException;

}
