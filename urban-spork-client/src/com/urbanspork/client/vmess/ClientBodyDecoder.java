package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ClientBodyDecoder extends ByteToMessageDecoder implements AEADPayloadDecoder {

    private static final Logger logger = LoggerFactory.getLogger(ClientBodyDecoder.class);

    private final AEADAuthenticator authenticator;
    private final ChunkSizeCodec chunkSizeCodec;
    private int payloadLength = INIT_PAYLOAD_LENGTH;

    public ClientBodyDecoder(AEADCipherCodec codec, ClientSession session) {
        chunkSizeCodec = new ClientAEADChunkSizeCodec(codec, session);
        authenticator = new AEADAuthenticator(codec, session.responseBodyKey, session.responseBodyIV);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        logger.debug("decode payload {}", ctx.channel());
        decodePayload(in, out);
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
    public int payloadLength() {
        return payloadLength;
    }

    @Override
    public void updatePayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Failed to decode response body", cause);
        super.exceptionCaught(ctx, cause);
    }

}
