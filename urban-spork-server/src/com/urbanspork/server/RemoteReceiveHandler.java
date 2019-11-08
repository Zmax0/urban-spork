package com.urbanspork.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class RemoteReceiveHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RemoteReceiveHandler.class);

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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        localChannel.writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught on channel " + ctx.channel() + " ~>", cause);
        ctx.close();
        release();
    }

    private void release() {
        if (localChannel != null) {
            localChannel.close();
            localChannel = null;
        }
    }

}
