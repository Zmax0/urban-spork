package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.chunk.ChunkSizeCodec;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

public interface PayloadEncoder {

    int payloadLimit();

    Authenticator auth();

    ChunkSizeCodec sizeCodec();

    PaddingLengthGenerator padding();

    /**
     * encrypt payload for TCP
     *
     * @param msg payload
     * @param out [salt][encrypted payload length][length tag][encrypted payload][payload tag]
     */
    default void encodePayload(ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        while (msg.isReadable()) {
            seal(msg, out);
        }
    }

    /**
     * encrypt payload for UDP
     *
     * @param msg payload
     * @param out [salt][encrypted payload][tag]
     */
    default void encodePacket(ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        seal(msg, out);
    }

    private void seal(ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        int paddingLength = 0;
        PaddingLengthGenerator padding = padding();
        if (padding != null) {
            paddingLength = padding.nextPaddingLength();
        }
        int encryptedSize = Math.min(msg.readableBytes(), payloadLimit() - auth().overhead() - sizeCodec().sizeBytes() - paddingLength);
        out.writeBytes(sizeCodec().encode(encryptedSize + paddingLength + auth().overhead()));
        byte[] in = new byte[encryptedSize];
        msg.readBytes(in);
        out.writeBytes(auth().seal(in));
        out.writeBytes(Dice.rollBytes(paddingLength));
    }
}
