package com.urbanspork.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class DefaultChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultChannelInboundHandler.class);

    private final Channel channel;

    public DefaultChannelInboundHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        channel.writeAndFlush(msg.retain());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught on channel " + ctx.channel() + " -> {}", cause.getMessage());
        ctx.close();
    }

}
