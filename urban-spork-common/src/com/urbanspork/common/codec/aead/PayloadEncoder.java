package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

import static com.urbanspork.common.codec.aead.CipherCodec.TAG_SIZE;

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
            int paddingLength = 0;
            PaddingLengthGenerator padding = padding();
            if (padding != null) {
                paddingLength = padding.nextPaddingLength();
            }
            int encryptedSize = Math.min(msg.readableBytes(), payloadLimit() - TAG_SIZE - sizeCodec().sizeBytes() - paddingLength);
            out.writeBytes(sizeCodec().encode(encryptedSize + paddingLength));
            byte[] in = new byte[encryptedSize];
            msg.readBytes(in);
            out.writeBytes(auth().seal(in));
            if (paddingLength > 0) {
                out.writeBytes(Dice.rollBytes(paddingLength));
            }
        }
    }

    /**
     * encrypt payload for UDP
     *
     * @param msg payload
     * @param out [salt][encrypted payload][tag]
     */
    default void encodePacket(ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        byte[] in = new byte[msg.readableBytes()];
        msg.readBytes(in);
        out.writeBytes(auth().seal(in));
    }
}
