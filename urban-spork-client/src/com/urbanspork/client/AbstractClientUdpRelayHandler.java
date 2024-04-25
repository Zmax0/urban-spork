package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.common.util.LruCache;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public abstract class AbstractClientUdpRelayHandler<K> extends SimpleChannelInboundHandler<DatagramPacketWrapper> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClientUdpRelayHandler.class);
    protected final ServerConfig config;
    private final LruCache<K, Channel> binding;

    protected AbstractClientUdpRelayHandler(ServerConfig config, Duration keepAlive) {
        super(false);
        this.config = config;
        this.binding = new LruCache<>(1024, keepAlive, (k, channel) -> {
            logger.info("[udp][binding][expire]{} != {}", k, channel.localAddress());
            channel.close();
        });
    }

    protected abstract Object convertToWrite(DatagramPacketWrapper msg);

    protected abstract K getKey(DatagramPacketWrapper msg);

    protected abstract Channel newBindingChannel(Channel inboundChannel, K k);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramPacketWrapper msg) {
        DatagramPacket packet = msg.packet();
        Channel inbound = ctx.channel();
        Channel outbound = getBindingChannel(inbound, getKey(msg));
        logger.info("[udp][{}]{}→{}~{}→{}", config.getProtocol(), packet.sender(), inbound.localAddress(), outbound.localAddress(), msg.proxy());
        outbound.writeAndFlush(convertToWrite(msg));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        logger.info("Stop timer and clear binding");
        binding.release();
    }

    private Channel getBindingChannel(Channel inboundChannel, K key) {
        return binding.computeIfAbsent(key, k -> {
            Channel channel = newBindingChannel(inboundChannel, key);
            channel.closeFuture().addListener(future -> {
                Channel removed = binding.remove(key);
                if (removed != null) {
                    logger.info("[udp][binding][close]{} != {}", key, removed.localAddress());
                }
            });
            return channel;
        });
    }
}
