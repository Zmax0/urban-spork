package com.urbanspork.server;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;

public class RemoteFrontendHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RemoteFrontendHandler.class);

    private final ByteBuf buff = Unpooled.directBuffer();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel localChannel = ctx.channel();
        InetSocketAddress remoteAddress = localChannel.attr(AttributeKeys.REMOTE_ADDRESS).get();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
            .group(localChannel.eventLoop())
            .channel(localChannel.getClass())
            .option(ChannelOption.AUTO_READ, false)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel remoteChannel) {
                    remoteChannel.pipeline().addLast(new RemoteBackendHandler(localChannel));
                }
            })
            .connect(remoteAddress)
            .addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    ChannelPipeline pipeline = localChannel.pipeline();
                    pipeline.addLast(new DefaultChannelInboundHandler(future.channel()));
                    if (pipeline.get(RemoteFrontendHandler.class) != null) {
                        pipeline.remove(RemoteFrontendHandler.this);
                        ctx.fireChannelRead(buff);
                    }
                } else {
                    logger.error("Connect " + remoteAddress + " failed");
                    localChannel.close();
                }
            });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        buff.writeBytes((ByteBuf) msg);
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ChannelCloseUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception on channel " + ctx.channel() + " ~>", cause);
        ChannelCloseUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.debug("channel unregistered");
        buff.release();
        super.channelUnregistered(ctx);
    }
}
