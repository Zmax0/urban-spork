package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.chunk.ChunkSizeCodec;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

public record PayloadEncoder(Authenticator auth, ChunkSizeCodec sizeCodec, PaddingLengthGenerator padding, int payloadLimit) {

    /**
     * encrypt payload for TCP
     */
    public void encodePayload(ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        while (msg.isReadable()) {
            seal(msg, out);
        }
    }

    /**
     * encrypt packet for UDP
     */
    public void encodePacket(ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        seal(msg, out);
    }

    private void seal(ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        int paddingLength = padding.nextPaddingLength();
        int overhead = auth.overhead();
        int sizeBytes = sizeCodec.sizeBytes();
        int encryptedSize = Math.min(msg.readableBytes(), payloadLimit - overhead - sizeBytes - paddingLength);
        if (sizeBytes > 0) {
            byte[] encodedSizeBytes = sizeCodec.encode(encryptedSize + paddingLength + overhead);
            out.writeBytes(encodedSizeBytes);
        }
        byte[] in = new byte[encryptedSize];
        msg.readBytes(in);
        out.writeBytes(auth.seal(in));
        if (paddingLength > 0) {
            out.writeBytes(Dice.rollBytes(paddingLength));
        }
    }
}
