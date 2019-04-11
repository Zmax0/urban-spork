package com.urbanspork.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class CachedChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final Logger logger = LoggerFactory.getLogger(CachedChannelInboundHandler.class);

    private final Channel channel;

    private ByteBuf cache;

    public CachedChannelInboundHandler(Channel channel, ByteBuf cache) {
        this.channel = channel;
        this.cache = cache;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (cache != null && cache.readableBytes() > 0) {
            ctx.writeAndFlush(cache);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        release();
        logger.debug("Channel {} inactive", ctx.channel());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        channel.writeAndFlush(msg.retain());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        release();
        logger.debug("Channel " + ctx.channel() + " error, cause: ", cause);
    }

    private void release() {
        channel.close();
        cache = null;
    }

}
