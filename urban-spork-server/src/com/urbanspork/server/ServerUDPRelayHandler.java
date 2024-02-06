package com.urbanspork.server;

import com.urbanspork.common.transport.udp.PacketEncoding;
import com.urbanspork.common.transport.udp.TernaryDatagramPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerUDPRelayHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(ServerUDPRelayHandler.class);
    private final Map<InetSocketAddress, Channel> workerChannels = new ConcurrentHashMap<>();
    private final EventLoopGroup workerGroup;
    private final PacketEncoding packetEncoding;

    public ServerUDPRelayHandler(PacketEncoding packetEncoding, EventLoopGroup workerGroup) {
        super(false);
        this.packetEncoding = packetEncoding;
        this.workerGroup = workerGroup;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        Channel channel = ctx.channel();
        InetSocketAddress callback = msg.recipient();
        Channel workerChannel = workerChannel(msg.sender(), channel);
        logger.info("[udp][relay]{}→{}~{}→{}", msg.sender(), callback, channel.localAddress(), workerChannel.localAddress());
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
                    ch.pipeline().addLast(
                        new IdleStateHandler(0, 0, 300),
                        new InboundHandler(channel)
                    );
                }
            })// callback->server->client
            .bind(0) // automatically assigned port now, may have security implications
            .syncUninterruptibly()
            .channel();
        workerChannel.closeFuture().addListener(future -> {
            Channel removed = workerChannels.remove(callback);
            if (removed != null) {
                logger.info("[udp][binding]{} != {}", callback, removed);
            }
        });
        logger.info("[udp][binding]{} == {}", callback, workerChannel);
        return workerChannel;
    }

    private static class InboundHandler extends MessageToMessageCodec<DatagramPacket, DatagramPacket> {

        private final Channel inboundChannel;
        private final Map<InetSocketAddress, InetSocketAddress> callbackMap = new ConcurrentHashMap<>();

        InboundHandler(Channel inboundChannel) {
            this.inboundChannel = inboundChannel;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
            callbackMap.put(msg.recipient(), msg.sender());
            out.add(msg.retain());
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
            InetSocketAddress sender = msg.sender();
            Channel outboundChannel = ctx.channel();
            InetSocketAddress callback = callbackMap.get(sender);
            if (callback != null) {
                logger.info("[udp][relay]{}←{}~{}←{}", callback, sender, inboundChannel.localAddress(), outboundChannel.localAddress());
                inboundChannel.writeAndFlush(new TernaryDatagramPacket(new DatagramPacket(msg.retain().content(), sender), callback));
            } else {
                logger.error("None callback of sender => {}", msg.sender());
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                ctx.close();
            }
        }
    }
}