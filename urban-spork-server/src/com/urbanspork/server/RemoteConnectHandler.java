package com.urbanspork.server;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.network.Network;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

class RemoteConnectHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RemoteConnectHandler.class);
    private final Bootstrap b = new Bootstrap();
    private final EventLoopGroup w = new NioEventLoopGroup(1);
    private final ServerConfig config;
    private Promise<Channel> p;

    public RemoteConnectHandler(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (Network.UDP.equals(ctx.channel().attr(AttributeKeys.NETWORK).get())) {
            udp(ctx, msg);
        } else {
            tcp(ctx, msg);
        }
    }

    private void udp(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Socks5CommandRequest request) {
            ctx.pipeline().addLast(
                new ServerUDPOverTCPCodec(request),
                new ServerUDPReplayHandler(config.getPacketEncoding(), w)
            );
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void tcp(ChannelHandlerContext ctx, Object msg) {
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
                        localChannel.pipeline().remove(RemoteConnectHandler.this);
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
                    logger.info("[tcp][{}][{}→{}]", config.getProtocol(), localChannel.localAddress(), remoteAddress);
                    p.setSuccess(future.channel());
                } else {
                    logger.error("[tcp][{}][{}→{}]", config.getProtocol(), localChannel.localAddress(), remoteAddress);
                    ctx.close();
                }
            });
    }
}