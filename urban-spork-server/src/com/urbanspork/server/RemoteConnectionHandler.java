package com.urbanspork.server;

import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.config.ServerConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class RemoteConnectionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RemoteConnectionHandler.class);
    private final Bootstrap b = new Bootstrap();
    private final ServerConfig config;
    private Promise<Channel> p;

    public RemoteConnectionHandler(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel localChannel = ctx.channel();
        if (msg instanceof InetSocketAddress address) {
            connect(ctx, localChannel, new InetSocketAddress(address.getHostString(), address.getPort()));
        } else if (msg instanceof Socks5CommandRequest request) {
            connect(ctx, localChannel, new InetSocketAddress(request.dstAddr(), request.dstPort()));
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

    private void connect(ChannelHandlerContext ctx, Channel localChannel, InetSocketAddress remoteAddress) {
        p = ctx.executor().newPromise();
        b.group(localChannel.eventLoop())
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(new DefaultChannelInboundHandler(localChannel))
            .connect(remoteAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    logger.info("[tcp][{}][{} → {}]", config.getProtocol(), localChannel.localAddress(), remoteAddress);
                    p.setSuccess(future.channel());
                } else {
                    logger.error("[tcp][{}][{} → {}]", config.getProtocol(), localChannel.localAddress(), remoteAddress);
                    ctx.close();
                }
            });
    }
}