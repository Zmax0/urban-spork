package com.urbanspork.protocol;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.common.Attributes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class ShadowsocksProtocolEncoder extends MessageToMessageEncoder<ByteBuf> {

    private final Logger logger = LoggerFactory.getLogger(ShadowsocksProtocolEncoder.class);

    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        msg.retain();
        Socks5CommandRequest request = ctx.channel().attr(Attributes.REQUEST).get();
        if (request != null) {
            logger.debug("Encode request: {}", request.dstAddr() + ':' + request.dstPort());
            ctx.channel().attr(Attributes.REQUEST).set(null);
            if (msg.readableBytes() > 0) {
                out.add(Unpooled.wrappedBuffer(ShadowsocksProtocol.encodeRequest(request), msg));
            } else {
                out.add(ShadowsocksProtocol.encodeRequest(request));
            }
        } else {
            out.add(msg);
        }
    }

}
