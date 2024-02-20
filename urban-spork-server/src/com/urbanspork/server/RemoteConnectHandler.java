package com.urbanspork.server;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.transport.Transport;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

class RemoteConnectHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RemoteConnectHandler.class);
    private final ServerConfig config;
    private Promise<Channel> p;

    public RemoteConnectHandler(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (Transport.UDP.equals(ctx.channel().attr(AttributeKeys.TRANSPORT).get())) {
            udp(ctx, msg);
        } else {
            tcp(ctx, msg);
        }
    }

    private void udp(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof InetSocketAddress address) {
            ctx.pipeline().addLast(
                new ServerUDPOverTCPCodec(address),
                new ServerUDPRelayHandler(config.getPacketEncoding(), ctx.channel().eventLoop().parent().next())
            );
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void tcp(ChannelHandlerContext ctx, Object msg) {
        Channel localChannel = ctx.channel();
        if (msg instanceof InetSocketAddress address) {
            p = ctx.executor().newPromise();
            connect(localChannel, address, p);
        } else {
            p.addListener((FutureListener<Channel>) future -> {
                if (future.isSuccess()) {
                    Channel remoteChannel = future.get();
                    localChannel.pipeline().addLast(new DefaultChannelInboundHandler(remoteChannel));
                    if (!ctx.isRemoved()) {
                        localChannel.pipeline().remove(RemoteConnectHandler.this);
                    }
                    remoteChannel.writeAndFlush(msg);
                } else {
                    ctx.close();
                }
            });
        }
    }

    private void connect(Channel localChannel, InetSocketAddress remoteAddress, Promise<Channel> promise) {
        new Bootstrap().group(localChannel.eventLoop())
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(new DefaultChannelInboundHandler(localChannel))
            .connect(remoteAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel remoteChannel = future.channel();
                    logger.info("[tcp][{}][{}→{}]", config.getProtocol(), localChannel.localAddress(), remoteChannel.remoteAddress());
                    promise.setSuccess(remoteChannel);
                } else {
                    logger.error("[tcp][{}][{}→{}]", config.getProtocol(), localChannel.localAddress(), remoteAddress);
                    promise.setFailure(future.cause());
                }
            });
    }
}