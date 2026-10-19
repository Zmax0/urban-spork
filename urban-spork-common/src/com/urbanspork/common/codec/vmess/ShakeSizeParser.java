package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.chunk.ChunkSizeCodec;
import com.urbanspork.common.crypto.Shake128;

public class ShakeSizeParser implements ChunkSizeCodec, PaddingLengthGenerator {

    private static final int MASK_BYTES = 2;
    private static final int BUFFERED_MASKS = 4096;

    private final Shake128 shake;
    private final byte[] buffer = new byte[MASK_BYTES * BUFFERED_MASKS];
    private int position = buffer.length;

    public ShakeSizeParser(byte[] nonce) {
        this.shake = new Shake128(nonce);
    }

    @Override
    public int sizeBytes() {
        return Short.BYTES;
    }

    @Override
    public byte[] encode(int size) {
        int mask = next();
        return new byte[]{(byte) ((mask ^ size) >>> 8), (byte) (mask ^ size)};
    }

    @Override
    public int decode(byte[] data) {
        int mask = next();
        int size = ((data[0] & 0xff) << 8) | (data[1] & 0xff);
        return mask ^ size;
    }

    @Override
    public int nextPaddingLength() {
        return next() % 64;
    }

    private int next() {
        if (position + MASK_BYTES > buffer.length) {
            shake.squeeze(buffer);
            position = 0;
        }
        int mask = ((buffer[position] & 0xff) << 8) | (buffer[position + 1] & 0xff);
        position += MASK_BYTES;
        return mask;
    }
}
