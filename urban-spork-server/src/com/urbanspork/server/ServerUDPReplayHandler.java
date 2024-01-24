package com.urbanspork.server;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.protocol.network.PacketEncoding;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerUDPReplayHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(ServerUDPReplayHandler.class);
    private final Map<InetSocketAddress, Channel> workerChannels = new ConcurrentHashMap<>();
    private final EventLoopGroup workerGroup;
    private final PacketEncoding packetEncoding;

    public ServerUDPReplayHandler(PacketEncoding packetEncoding, EventLoopGroup workerGroup) {
        super(false);
        this.packetEncoding = packetEncoding;
        this.workerGroup = workerGroup;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        Channel channel = ctx.channel();
        InetSocketAddress callback = msg.recipient();
        Channel workerChannel = workerChannel(callback, channel);
        workerChannel.attr(AttributeKeys.CALLBACK).get().put(callback, msg.sender());
        logger.info("[udp][replay]{}→{}~{}→{}", msg.sender(), callback, channel.localAddress(), workerChannel.localAddress());
        workerChannel.writeAndFlush(msg);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        for (Map.Entry<?, Channel> entry : workerChannels.entrySet()) {
            entry.getValue().close();
        }
    }

    Channel workerChannel(InetSocketAddress callback, Channel inboundChannel) {
        if (PacketEncoding.Packet == packetEncoding) {
            callback = PacketEncoding.Packet.seqPacketMagicAddress();
        }
        return workerChannels.computeIfAbsent(callback, key -> newWorkerChannel(key, inboundChannel));
    }

    private Channel newWorkerChannel(InetSocketAddress callback, Channel channel) {
        Channel workerChannel = new Bootstrap().group(workerGroup).channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.attr(AttributeKeys.CALLBACK).set(new ConcurrentHashMap<>());
                    ch.pipeline().addLast(
                        new IdleStateHandler(0, 0, 120),
                        new InboundHandler(channel)
                    );
                }
            })// callback->server->client
            .bind(0) // automatically assigned port now, may have security implications
            .syncUninterruptibly().channel();
        workerChannel.closeFuture().addListener(future -> {
            Channel removed = workerChannels.remove(callback);
            if (removed != null) {
                logger.info("[udp][binding]{} != {}", callback, removed.localAddress());
            }
        });
        logger.info("[udp][binding]{} == {}", callback, workerChannel.localAddress());
        return workerChannel;
    }

    private static class InboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        private final Channel inboundChannel;

        InboundHandler(Channel inboundChannel) {
            super(false);
            this.inboundChannel = inboundChannel;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
            InetSocketAddress sender = msg.sender();
            Channel outboundChannel = ctx.channel();
            InetSocketAddress callback = outboundChannel.attr(AttributeKeys.CALLBACK).get().get(sender);
            if (callback != null) {
                logger.info("[udp][replay]{}←{}~{}←{}", callback, sender, inboundChannel.localAddress(), outboundChannel.localAddress());
                inboundChannel.writeAndFlush(new TernaryDatagramPacket(new DatagramPacket(msg.content(), sender), callback));
            } else {
                logger.error("None callback of sender => {}", msg.sender());
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                ctx.close();
                inboundChannel.close();
            }
        }
    }
}