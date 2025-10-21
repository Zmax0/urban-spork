package com.urbanspork.server;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.common.transport.udp.PacketEncoding;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ServerUdpRelayHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final Logger logger = LoggerFactory.getLogger(ServerUdpRelayHandler.class);
    private final Map<Object, Channel> workerChannels = new ConcurrentHashMap<>();
    private final EventLoopGroup workerGroup;
    private final PacketEncoding packetEncoding;

    public ServerUdpRelayHandler(PacketEncoding packetEncoding, EventLoopGroup workerGroup) {
        super(false);
        this.packetEncoding = packetEncoding;
        this.workerGroup = workerGroup;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        Channel channel = ctx.channel();
        InetSocketAddress sender = msg.sender();
        InetSocketAddress recipient = msg.recipient();
        Object key = channel.attr(AttributeKeys.SERVER_UDP_RELAY_WORKER).get();
        if (key == null) {
            key = sender;
        }
        Channel workerChannel = workerChannel(key, channel);
        logger.info("[udp][relay]{}→{}~{}→{}", sender, recipient, channel.localAddress(), workerChannel.localAddress());
        workerChannel.writeAndFlush(msg);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        for (Map.Entry<?, Channel> entry : workerChannels.entrySet()) {
            entry.getValue().close();
        }
    }

    Channel workerChannel(Object key, Channel inboundChannel) {
        if (PacketEncoding.Packet == packetEncoding) {
            key = PacketEncoding.Packet.seqPacketMagicAddress();
        }
        return workerChannels.computeIfAbsent(key, k -> newWorkerChannel(k, inboundChannel));
    }

    private Channel newWorkerChannel(Object key, Channel channel) {
        Channel workerChannel = new Bootstrap().group(workerGroup).channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        new IdleStateHandler(0, 0, 300),
                        new InboundHandler(channel)
                    );
                }
            }) // callback->server->client
            .bind(0) // automatically assigned port now, may have security implications
            .syncUninterruptibly()
            .channel();
        workerChannel.closeFuture().addListener(_ -> logger.info("[udp][binding]{} != {}", key, workerChannels.remove(key)));
        logger.info("[udp][binding]{} == {}", key, workerChannel);
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
            Optional.ofNullable(msg.sender()).ifPresent(sender -> callbackMap.put(msg.recipient(), sender));
            out.add(msg.retain());
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
            InetSocketAddress callback = msg.sender(); // reverse naming
            InetSocketAddress sender = this.callbackMap.get(callback);
            logger.info("[udp][relay]{}←{}~{}←{}", sender, callback, inboundChannel.localAddress(), ctx.channel().localAddress());
            inboundChannel.writeAndFlush(new DatagramPacketWrapper(new DatagramPacket(msg.retain().content(), callback), sender));
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                ctx.close();
            }
        }
    }
}