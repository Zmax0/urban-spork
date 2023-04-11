package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.ChunkSizeCodec;
import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

public interface AEADPayloadEncoder {

    int payloadLimit();

    AEADAuthenticator payloadEncoder();

    ChunkSizeCodec chunkSizeEncoder();

    default void encodePayload(ByteBuf msg, ByteBuf out) throws InvalidCipherTextException {
        while (msg.isReadable()) {
            int length = Math.min(msg.readableBytes(), payloadLimit());
            out.writeBytes(chunkSizeEncoder().encode(length));
            byte[] in = new byte[length];
            msg.readBytes(in);
            out.writeBytes(payloadEncoder().seal(in));
        }
    }
}
