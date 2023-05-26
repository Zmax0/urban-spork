package com.urbanspork.client.shadowsocks;

import com.urbanspork.common.codec.shadowsocks.ShadowsocksUDPReplayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.network.TernaryDatagramPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClientUDPReplayHandler extends SimpleChannelInboundHandler<TernaryDatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(ClientUDPReplayHandler.class);
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private static final Map<InetSocketAddress, Channel> binding = new ConcurrentHashMap<>();
    private static final HashedWheelTimer timer = new HashedWheelTimer(1, TimeUnit.SECONDS);
    private final ServerConfig config;
    private final InetSocketAddress replay;

    public ClientUDPReplayHandler(ServerConfig config) {
        super(false);
        this.config = config;
        this.replay = new InetSocketAddress(config.getHost(), config.getPort());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, TernaryDatagramPacket msg) {
        DatagramPacket packet = msg.packet();
        InetSocketAddress sender = packet.sender();
        getBindingChannel(ctx.channel(), sender).writeAndFlush(new TernaryDatagramPacket(new DatagramPacket(packet.content(), msg.third()), replay));
    }

    private Channel getBindingChannel(Channel inboundChannel, InetSocketAddress sender) {
        return binding.computeIfAbsent(sender, key -> {
            Channel channel = newBindingChannel(inboundChannel, sender);
            logger.info("New binding => {} - {}", sender, channel.localAddress());
            timer.newTimeout(timeout -> channel.close(), 10, TimeUnit.MINUTES);
            timer.newTimeout(timeout -> binding.remove(sender), 1, TimeUnit.MINUTES);
            return channel;
        });
    }

    private Channel newBindingChannel(Channel inboundChannel, InetSocketAddress sender) {
        return new Bootstrap().group(workerGroup).channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        new ShadowsocksUDPReplayCodec(config),
                        new InboundHandler(inboundChannel, sender)// server->client->sender
                    );
                }
            }).bind(0) // automatically assigned port now, may have security implications
            .syncUninterruptibly().channel();
    }

    private static class InboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        private final Channel channel;
        private final InetSocketAddress sender;

        InboundHandler(Channel channel, InetSocketAddress sender) {
            super(false);
            this.channel = channel;
            this.sender = sender;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            logger.info("Replay {} <- {} via {} <- {}", sender, packet.recipient(), ctx.channel().localAddress(), packet.sender());
            channel.writeAndFlush(new TernaryDatagramPacket(new DatagramPacket(packet.content(), packet.recipient()), sender));
        }
    }
}
