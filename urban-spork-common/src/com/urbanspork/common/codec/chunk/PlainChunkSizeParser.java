package com.urbanspork.common.codec.chunk;

import io.netty.buffer.Unpooled;

public class PlainChunkSizeParser implements ChunkSizeCodec {

    @Override
    public int sizeBytes() {
        return Short.BYTES;
    }

    @Override
    public byte[] encode(int size) {
        byte[] bytes = new byte[Short.BYTES];
        Unpooled.wrappedBuffer(bytes).setShort(0, size);
        return bytes;
    }

    @Override
    public int decode(byte[] data) {
        return Unpooled.wrappedBuffer(data).readUnsignedShort();
    }
}
