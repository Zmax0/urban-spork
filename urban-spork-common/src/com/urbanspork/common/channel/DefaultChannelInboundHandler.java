package com.urbanspork.common.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class DefaultChannelInboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultChannelInboundHandler.class);

    protected final Channel channel;

    public DefaultChannelInboundHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (channel.isActive()) {
            channel.writeAndFlush(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught on channel " + ctx.channel() + " -> {}", cause);
        ChannelCloseUtils.closeOnFlush(channel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.trace("Inbound channel {} inactive", ctx.channel());
        ChannelCloseUtils.closeOnFlush(channel);
    }
}
