package com.urbanspork.common.codec.chunk;

import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

public class PlainChunkSizeParser implements ChunkSizeCodec {

    @Override
    public int sizeBytes() {
        return Short.BYTES;
    }

    @Override
    public byte[] encode(int size) throws InvalidCipherTextException {
        byte[] bytes = new byte[Short.BYTES];
        Unpooled.wrappedBuffer(bytes).setShort(0, size);
        return bytes;
    }

    @Override
    public int decode(byte[] data) throws InvalidCipherTextException {
        return Unpooled.wrappedBuffer(data).readUnsignedShort();
    }
}
