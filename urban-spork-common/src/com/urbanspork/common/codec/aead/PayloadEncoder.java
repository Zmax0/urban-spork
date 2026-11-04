package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.chunk.ChunkSizeCodec;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;

public record PayloadEncoder(Authenticator auth, ChunkSizeCodec sizeCodec, PaddingLengthGenerator padding, int payloadLimit) implements AutoCloseable {

    /**
     * encrypt payload for TCP
     */
    public void encodePayload(ByteBuf msg, ByteBuf out) {
        while (msg.isReadable()) {
            seal(msg, out);
        }
    }

    /**
     * encrypt packet for UDP
     */
    public void encodePacket(ByteBuf msg, ByteBuf out) {
        seal(msg, out);
    }

    private void seal(ByteBuf msg, ByteBuf out) {
        int paddingLength = padding.nextPaddingLength();
        int overhead = auth.overhead();
        int sizeBytes = sizeCodec.sizeBytes();
        int encryptedSize = Math.min(msg.readableBytes(), payloadLimit - overhead - sizeBytes - paddingLength);
        if (sizeBytes > 0) {
            byte[] encodedSizeBytes = sizeCodec.encode(encryptedSize + paddingLength + overhead);
            out.writeBytes(encodedSizeBytes);
        }
        int index = out.writerIndex();
        out.ensureWritable(encryptedSize + overhead);
        msg.readBytes(out, index, encryptedSize);
        auth.seal(out, index, encryptedSize + overhead);
        out.writerIndex(index + encryptedSize + overhead);
        if (paddingLength > 0) {
            out.writeBytes(Dice.rollBytes(paddingLength));
        }
    }

    @Override
    public void close() throws Exception {
        auth.close();
        sizeCodec.close();
    }
}
