package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.PaddingLengthGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.digests.SHAKEDigest;

public class ShakeSizeParser implements ChunkSizeCodec, PaddingLengthGenerator {

    private final SHAKEDigest shake = new SHAKEDigest();

    private final ByteBuf buffer = Unpooled.buffer(sizeBytes());

    public ShakeSizeParser(byte[] nonce) {
        shake.update(nonce, 0, nonce.length);
    }

    @Override
    public int sizeBytes() {
        return Short.BYTES;
    }

    @Override
    public byte[] encode(int size) {
        byte[] result = new byte[2];
        int mask = next();
        Unpooled.wrappedBuffer(result).setShort(0, mask ^ size);
        return result;
    }

    @Override
    public int decode(byte[] data) {
        int mask = next();
        int size = Unpooled.wrappedBuffer(data).readUnsignedShort();
        return mask ^ size;
    }

    @Override
    public int maxPaddingLength() {
        return 64;
    }

    @Override
    public int nextPaddingLength() {
        return next() % 64;
    }

    private int next() {
        shake.doOutput(buffer.array(), 0, sizeBytes());
        return buffer.getUnsignedShort(0);
    }
}
