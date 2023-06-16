package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.PaddingLengthGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.List;

public interface PayloadDecoder {

    int INIT_PAYLOAD_LENGTH = -1;

    int payloadLength();

    void updatePayloadLength(int payloadLength);

    Authenticator auth();

    ChunkSizeCodec sizeCodec();

    PaddingLengthGenerator padding();

    /**
     * decrypt payload for TCP
     *
     * @param in  [salt][encrypted payload length][length tag][encrypted payload][payload tag]
     * @param out payload
     */
    default void decodePayload(ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        ChunkSizeCodec chunkSizeDecoder = sizeCodec();
        int payloadLength = payloadLength();
        while (in.readableBytes() >= (payloadLength == INIT_PAYLOAD_LENGTH ? chunkSizeDecoder.sizeBytes() : payloadLength + CipherCodec.TAG_SIZE)) {
            if (payloadLength == INIT_PAYLOAD_LENGTH) {
                byte[] payloadSizeBytes = new byte[chunkSizeDecoder.sizeBytes()];
                in.readBytes(payloadSizeBytes);
                payloadLength = chunkSizeDecoder.decode(payloadSizeBytes);
            } else {
                int paddingLength = 0;
                PaddingLengthGenerator padding = padding();
                if (padding != null) {
                    paddingLength = padding.nextPaddingLength();
                }
                byte[] payloadBytes = new byte[payloadLength + CipherCodec.TAG_SIZE - paddingLength];
                in.readBytes(payloadBytes);
                in.skipBytes(paddingLength);
                out.add(Unpooled.wrappedBuffer(auth().open(payloadBytes)));
                payloadLength = INIT_PAYLOAD_LENGTH;
            }
            updatePayloadLength(payloadLength);
        }
    }

    /**
     * decrypt packet for UDP
     *
     * @param in  [salt][encrypted payload][tag]
     * @param out payload
     */
    default void decodePacket(ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        byte[] payloadBytes = new byte[in.readableBytes()];
        in.readBytes(payloadBytes);
        out.add(Unpooled.wrappedBuffer(auth().open(payloadBytes)));
    }
}