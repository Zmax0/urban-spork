package com.urbanspork.common.codec;

public interface ChunkSizeCodec {

    default int sizeBytes() {
        return Short.BYTES;
    }

    byte[] encode(int size) throws Exception;

    int decode(byte[] data) throws Exception;

}
