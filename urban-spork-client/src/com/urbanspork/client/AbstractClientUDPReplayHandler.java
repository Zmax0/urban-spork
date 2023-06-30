package com.urbanspork.client;

import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class AbstractClientUDPReplayHandler<K> extends SimpleChannelInboundHandler<TernaryDatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClientUDPReplayHandler.class);
    private final HashedWheelTimer timer = new HashedWheelTimer(1, TimeUnit.SECONDS);
    private final Map<K, Channel> binding = new ConcurrentHashMap<>();
    protected final ServerConfig config;

    protected AbstractClientUDPReplayHandler(ServerConfig config) {
        super(false);
        this.config = config;
    }

    protected abstract Object convertToWrite(TernaryDatagramPacket msg);

    protected abstract K getKey(TernaryDatagramPacket msg);

    protected abstract Channel newBindingChannel(Channel inboundChannel, K k);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, TernaryDatagramPacket msg) {
        DatagramPacket packet = msg.packet();
        Channel inbound = ctx.channel();
        Channel outbound = getBindingChannel(inbound, getKey(msg));
        logger.info("[udp][{}]{}→{}~{}→{}", config.getProtocol(), packet.sender(), inbound.localAddress(), outbound.localAddress(), msg.third());
        outbound.writeAndFlush(convertToWrite(msg));
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        ctx.fireChannelUnregistered();
        logger.info("Stop timer and clean binding");
        timer.stop();
        ChannelCloseUtils.clearMap(binding);
    }

    private Channel getBindingChannel(Channel inboundChannel, K key) {
        return binding.computeIfAbsent(key, k -> {
            Channel channel = newBindingChannel(inboundChannel, key);
            logger.info("New binding => {} - {}", key, channel.localAddress());
            timer.newTimeout(timeout -> channel.close(), 10, TimeUnit.MINUTES);
            timer.newTimeout(timeout -> binding.remove(key), 1, TimeUnit.MINUTES);
            return channel;
        });
    }

}
