package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.ChunkSizeCodec;
import io.netty.buffer.ByteBuf;

import java.util.List;

public interface AEADPayloadDecoder extends AEADPayloadCodec {

    int INIT_PAYLOAD_LENGTH = -1;

    int payloadLength();

    void updatePayloadLength(int payloadLength);

    default void decodePayload(ByteBuf in, List<Object> out) throws Exception {
        ChunkSizeCodec chunkSizeCodec = chunkSizeCodec();
        AEADAuthenticator authenticator = authenticator();
        int payloadLength = payloadLength();
        while (in.readableBytes() >= (payloadLength == INIT_PAYLOAD_LENGTH ? chunkSizeCodec.sizeBytes() + AEADCipherCodec.TAG_SIZE : payloadLength + AEADCipherCodec.TAG_SIZE)) {
            if (payloadLength == INIT_PAYLOAD_LENGTH) {
                byte[] payloadSizeBytes = new byte[chunkSizeCodec.sizeBytes() + AEADCipherCodec.TAG_SIZE];
                in.readBytes(payloadSizeBytes);
                updatePayloadLength(chunkSizeCodec.decode(payloadSizeBytes));
            } else {
                byte[] payloadBytes = new byte[payloadLength + AEADCipherCodec.TAG_SIZE];
                in.readBytes(payloadBytes);
                out.add(in.alloc().buffer().writeBytes(authenticator.open(payloadBytes)));
                updatePayloadLength(INIT_PAYLOAD_LENGTH);
            }
            payloadLength = payloadLength();
        }
    }
}
