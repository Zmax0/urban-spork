package com.urbanspork.server;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class RemoteConnectHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RemoteConnectHandler.class);

    private ByteBuf buff = Unpooled.directBuffer();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        connect(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            buff.writeBytes((ByteBuf) msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception on channel " + ctx.channel() + " ~>", cause);
        ctx.close();
        ctx.newFailedFuture(cause);
    }

    private void connect(ChannelHandlerContext ctx) {
        Channel localChannel = ctx.channel();
        InetSocketAddress remoteAddress = localChannel.attr(AttributeKeys.REMOTE_ADDRESS).get();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
            .group(localChannel.eventLoop())
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel remoteChannel) throws Exception {
                    remoteChannel.pipeline().addLast(new DefaultChannelInboundHandler(localChannel));
                }
            })
            .connect(remoteAddress)
            .addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    localChannel.pipeline()
                        .addLast(new DefaultChannelInboundHandler(future.channel()))
                        .remove(RemoteConnectHandler.this);
                    ctx.fireChannelRead(buff);
                    buff = null;
                } else {
                    logger.error("Connect " + remoteAddress + " failed");
                    ctx.close();
                }
            });
    }

}
