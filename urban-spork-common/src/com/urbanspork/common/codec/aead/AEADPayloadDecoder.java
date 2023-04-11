package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.ChunkSizeCodec;
import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.List;

public interface AEADPayloadDecoder {

    int INIT_PAYLOAD_LENGTH = -1;

    int payloadLength();

    void updatePayloadLength(int payloadLength);

    AEADAuthenticator payloadDecoder();

    ChunkSizeCodec chunkSizeDecoder();

    default void decodePayload(ByteBuf in, List<Object> out) throws InvalidCipherTextException {
        ChunkSizeCodec chunkSizeDecoder = chunkSizeDecoder();
        int payloadLength = payloadLength();
        while (in.readableBytes() >= (payloadLength == INIT_PAYLOAD_LENGTH ? chunkSizeDecoder.sizeBytes() + AEADCipherCodec.TAG_SIZE : payloadLength + AEADCipherCodec.TAG_SIZE)) {
            if (payloadLength == INIT_PAYLOAD_LENGTH) {
                byte[] payloadSizeBytes = new byte[chunkSizeDecoder.sizeBytes() + AEADCipherCodec.TAG_SIZE];
                in.readBytes(payloadSizeBytes);
                payloadLength = chunkSizeDecoder.decode(payloadSizeBytes);
            } else {
                byte[] payloadBytes = new byte[payloadLength + AEADCipherCodec.TAG_SIZE];
                in.readBytes(payloadBytes);
                out.add(in.alloc().buffer().writeBytes(payloadDecoder().open(payloadBytes)));
                payloadLength = INIT_PAYLOAD_LENGTH;
            }
            updatePayloadLength(payloadLength);
        }
    }
}
