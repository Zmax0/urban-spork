package com.urbanspork.server.shadowsocks;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.shadowsocks.network.PacketEncoding;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerUDPReplayHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServerUDPReplayHandler.class);
    private final Map<InetSocketAddress, Channel> workerChannels = new ConcurrentHashMap<>();
    private final EventLoopGroup workerGroup;
    private final PacketEncoding packetEncoding;

    public ServerUDPReplayHandler(PacketEncoding packetEncoding, EventLoopGroup workerGroup) {
        this.packetEncoding = packetEncoding;
        this.workerGroup = workerGroup;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof DatagramPacket packet) {
            Channel channel = ctx.channel();
            InetSocketAddress callback = packet.recipient();
            Channel workerChannel = workerChannel(callback, channel);
            workerChannel.attr(AttributeKeys.CALLBACK).get().put(callback, packet.sender());
            logger.info("Replay {} -> {} via {} -> {}", packet.sender(), callback, channel.localAddress(), workerChannel.localAddress());
            workerChannel.writeAndFlush(packet);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        ctx.fireChannelUnregistered();
        ChannelCloseUtils.clearMap(workerChannels);
    }

    Channel workerChannel(InetSocketAddress callback, Channel inboundChannel) {
        if (PacketEncoding.Packet == packetEncoding) {
            callback = PacketEncoding.Packet.seqPacketMagicAddress();
        }
        return workerChannels.computeIfAbsent(callback, key -> newWorkerChannel(key, inboundChannel));
    }

    private Channel newWorkerChannel(InetSocketAddress callback, Channel channel) {
        Channel outboundChannel = new Bootstrap().group(workerGroup).channel(NioDatagramChannel.class)
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
        logger.info("New working binding: {} == {}", callback, outboundChannel.localAddress());
        return outboundChannel;
    }

    private class InboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

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
                logger.info("Replay {} <- {} via {} <- {}", callback, sender, inboundChannel.localAddress(), outboundChannel.localAddress());
                inboundChannel.writeAndFlush(new TernaryDatagramPacket(new DatagramPacket(msg.content(), sender), callback));
            } else {
                logger.error("None callback of sender => {}", msg.sender());
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                Channel channel = ctx.channel();
                workerChannels.entrySet().removeIf(entry -> {
                    Channel value = entry.getValue();
                    boolean flag = channel.equals(value);
                    if (flag) {
                        logger.info("Remove working binding: {} != {}", entry.getKey(), channel.localAddress());
                        if (value.isActive()) {
                            value.close();
                        }
                    }
                    return flag;
                });
            }
        }
    }
}