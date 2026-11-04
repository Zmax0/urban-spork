package com.urbanspork.common.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultChannelInboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultChannelInboundHandler.class);

    private final Channel channel;
    private final boolean autoRead;
    private final ChannelInboundHandlerAdapter writabilityHandler = new ChannelInboundHandlerAdapter() {
        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) {
            ctx.fireChannelWritabilityChanged();
            requestRead();
        }
    };
    private ChannelHandlerContext sourceContext;
    private boolean readPending;

    public DefaultChannelInboundHandler(Channel channel, boolean autoRead) {
        this.channel = channel;
        this.autoRead = autoRead;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        if (autoRead) {
            return;
        }
        sourceContext = ctx;
        ctx.channel().config().setAutoRead(false);
        channel.pipeline().addLast(writabilityHandler);
        ctx.executor().execute(this::requestRead);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.fireChannelActive();
        if (!autoRead) {
            requestRead();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!channel.isActive()) {
            ReferenceCountUtil.release(msg);
            ctx.close();
            return;
        }
        channel.writeAndFlush(msg).addListener(future -> {
            if (!future.isSuccess()) {
                ctx.close();
            }
        });
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.fireChannelReadComplete();
        if (!autoRead) {
            readPending = false;
            requestRead();
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        sourceContext = null;
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.context(writabilityHandler) != null) {
            pipeline.remove(writabilityHandler);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (channel.isActive()) {
            ChannelCloseUtil.closeOnFlush(channel);
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Relay exception: source={} target={}", ctx.channel(), channel, cause);
        channel.close();
        ctx.close();
    }

    private void requestRead() {
        if (sourceContext == null) {
            return;
        }
        readIfPossible(sourceContext);
    }

    private void readIfPossible(ChannelHandlerContext ctx) {
        if (readPending || !ctx.channel().isActive() || !channel.isActive() || !channel.isWritable()) {
            return;
        }
        readPending = true;
        ctx.read();
    }
}
