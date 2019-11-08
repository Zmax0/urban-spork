package com.urbanspork.common.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class DefaultChannelInboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultChannelInboundHandler.class);

    private final Channel channel;

    public DefaultChannelInboundHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        channel.writeAndFlush(msg);
        ctx.fireChannelReadComplete();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught on channel " + ctx.channel() + " -> {}", cause.getMessage());
        ctx.close();
        channel.close();
    }

}
