package com.urbanspork.common.protocol;

import com.urbanspork.common.channel.AttributeKeys;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShadowsocksProtocolEncoder extends MessageToByteEncoder<ByteBuf> implements ShadowsocksProtocol {

    private final Logger logger = LoggerFactory.getLogger(ShadowsocksProtocolEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        Socks5CommandRequest request = ctx.channel().attr(AttributeKeys.REQUEST).get();
        if (request != null) {
            logger.debug("Encode request: {}", request.dstAddr() + ':' + request.dstPort());
            ctx.channel().attr(AttributeKeys.REQUEST).set(null);
            out.writeBytes(encodeRequest(request));
            if (msg.isReadable()) {
                out.writeBytes(msg);
            }
        } else {
            out.writeBytes(msg);
        }
    }
}
