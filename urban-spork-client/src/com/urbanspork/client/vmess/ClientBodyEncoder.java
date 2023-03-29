package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


public class ClientBodyEncoder extends MessageToByteEncoder<ByteBuf> implements AEADPayloadEncoder {

    private final AEADAuthenticator authenticator;
    private final ChunkSizeCodec chunkSizeCodec;

    public ClientBodyEncoder(AEADCipherCodec codec, ClientSession session) {
        chunkSizeCodec = new ClientAEADChunkSizeCodec(codec, session);
        authenticator = new AEADAuthenticator(codec, session.requestBodyKey, session.requestBodyIV);
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        encodePayload(msg, out);
    }

    @Override
    public ChunkSizeCodec chunkSizeCodec() {
        return chunkSizeCodec;
    }

    @Override
    public AEADAuthenticator authenticator() {
        return authenticator;
    }

    @Override
    public int maxPayloadLength() {
        return 65535;
    }
}
