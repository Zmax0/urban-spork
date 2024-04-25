package com.urbanspork.server;

import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.transport.tcp.RelayingPayload;
import com.urbanspork.common.transport.udp.RelayingPacket;
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

class ServerRelayHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServerRelayHandler.class);
    private final ServerConfig config;
    private Promise<Channel> p;

    public ServerRelayHandler(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof RelayingPayload<?> payload) {
            relayTcp(ctx, payload);
        } else if (msg instanceof RelayingPacket<?> pocket) {
            relayUdp(ctx, pocket);
        } else { // should always relay tcp
            p.addListener(future -> ctx.fireChannelRead(msg));
        }
    }

    private void relayUdp(ChannelHandlerContext ctx, RelayingPacket<?> relayingPayload) {
        ctx.pipeline().remove(this).addLast(
            new ServerUdpOverTcpCodec(relayingPayload.address()),
            new ServerUdpRelayHandler(config.getPacketEncoding(), ctx.channel().eventLoop().parent().next())
        );
        ctx.fireChannelRead(relayingPayload.content());
    }

    private void relayTcp(ChannelHandlerContext ctx, RelayingPayload<?> relayingPayload) {
        Channel localChannel = ctx.channel();
        p = ctx.executor().newPromise();
        connect(localChannel, relayingPayload.address(), p);
        p.addListener((FutureListener<Channel>) future -> {
            if (future.isSuccess()) {
                Channel remoteChannel = future.get();
                localChannel.pipeline().remove(this).addLast(new DefaultChannelInboundHandler(remoteChannel));
                remoteChannel.writeAndFlush(relayingPayload.content());
            } else {
                ctx.close();
            }
        });
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