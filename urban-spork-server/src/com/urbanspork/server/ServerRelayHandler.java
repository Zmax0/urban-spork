package com.urbanspork.server;

import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.transport.tcp.RelayingPayload;
import com.urbanspork.common.transport.udp.RelayingPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ServerRelayHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServerRelayHandler.class);
    private final ServerConfig config;
    private ChannelFuture future;

    public ServerRelayHandler(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        switch (msg) {
            case RelayingPacket<?> packet -> relayUdp(ctx, packet);
            case RelayingPayload<?> payload -> relayTcp(ctx, payload);
            default -> Objects.requireNonNull(future, "expect a RelayingPayload for the first reading")
                .addListener((ChannelFutureListener) f -> f.channel().writeAndFlush(msg)); // should always relay tcp
        }
    }

    private void relayUdp(ChannelHandlerContext ctx, RelayingPacket<?> relayingPacket) {
        ctx.pipeline().addLast(
            new ServerUdpOverTcpCodec(relayingPacket.address()),
            new ServerUdpRelayHandler(config.getPacketEncoding(), ctx.channel().eventLoop().parent().next())
        ).remove(this);
        ctx.fireChannelRead(relayingPacket.content());
    }

    private void relayTcp(ChannelHandlerContext ctx, RelayingPayload<?> relayingPayload) {
        future = connect(ctx.channel(), relayingPayload);
    }

    private ChannelFuture connect(Channel localChannel, RelayingPayload<?> relayingPayload) {
        return new Bootstrap().group(localChannel.eventLoop())
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(new DefaultChannelInboundHandler(localChannel))
            .connect(relayingPayload.address())
            .addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    Channel remoteChannel = f.channel();
                    if (localChannel.isActive()) {
                        localChannel.pipeline().addLast(new DefaultChannelInboundHandler(remoteChannel)).remove(ServerRelayHandler.class);
                        logger.info("[tcp][{}][{}→{}]", config.getProtocol(), localChannel.localAddress(), remoteChannel.remoteAddress());
                        remoteChannel.writeAndFlush(relayingPayload.content());
                    } else {
                        logger.error("[tcp][{}][{}→{}] client close", config.getProtocol(), localChannel.localAddress(), relayingPayload.address());
                        remoteChannel.close();
                    }
                } else {
                    logger.error("[tcp][{}][{}→{}] connect peer failed", config.getProtocol(), localChannel.localAddress(), relayingPayload.address());
                    localChannel.close();
                }
            });
    }
}