package com.urbanspork.common.codec.aead;

import io.netty.buffer.ByteBuf;

public interface AEADPayloadEncoder extends AEADPayloadCodec {

    int maxPayloadLength();

    default void encodePayload(ByteBuf msg, ByteBuf out) throws Exception {
        while (msg.isReadable()) {
            int length = Math.min(msg.readableBytes(), maxPayloadLength());
            out.writeBytes(chunkSizeCodec().encode(length));
            byte[] in = new byte[length];
            msg.readBytes(in);
            out.writeBytes(authenticator().seal(in));
        }
    }
}
