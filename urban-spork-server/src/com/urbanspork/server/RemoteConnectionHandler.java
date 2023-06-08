package com.urbanspork.server;

import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class RemoteConnectionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RemoteConnectionHandler.class);
    private final Bootstrap b = new Bootstrap();
    private Promise<Channel> p;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel localChannel = ctx.channel();
        if (msg instanceof InetSocketAddress remoteAddress) {
            p = ctx.executor().newPromise();
            b.group(localChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .handler(new DefaultChannelInboundHandler(localChannel))
                .connect(remoteAddress).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        logger.info("Connect success [id: {}, L: {} - R: {}]", localChannel.id(), localChannel.localAddress(), remoteAddress);
                        p.setSuccess(future.channel());
                    } else {
                        logger.error("Connect failed [id: {}, L: {} - R: {}]", localChannel.id(), localChannel.localAddress(), remoteAddress);
                        ctx.close();
                    }
                });
        } else {
            p.addListener((FutureListener<Channel>) future -> {
                    Channel outboundChannel = future.get();
                    localChannel.pipeline().addLast(new DefaultChannelInboundHandler(outboundChannel));
                    if (!ctx.isRemoved()) {
                        localChannel.pipeline().remove(RemoteConnectionHandler.this);
                    }
                    outboundChannel.writeAndFlush(msg);
                }
            );
        }
    }
}