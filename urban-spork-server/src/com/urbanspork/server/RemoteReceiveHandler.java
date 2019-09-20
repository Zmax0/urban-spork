package com.urbanspork.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RemoteReceiveHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final Logger logger = LoggerFactory.getLogger(RemoteReceiveHandler.class);

    private Channel localChannel;
    private ByteBuf buff;

    public RemoteReceiveHandler(Channel localChannel, ByteBuf buff) {
        this.localChannel = localChannel;
        this.buff = buff;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(buff);
        buff = null;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        release();
        logger.info("Channel {} inactive", ctx.channel());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        localChannel.writeAndFlush(msg.retain());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught on channel " + ctx.channel() + " -> {}", cause.getMessage());
        ctx.close();
        release();
    }

    private void release() {
        if (localChannel != null) {
            localChannel.close();
            localChannel = null;
        }
        if (buff != null) {
            buff.release();
            buff = null;
        }
    }

}
