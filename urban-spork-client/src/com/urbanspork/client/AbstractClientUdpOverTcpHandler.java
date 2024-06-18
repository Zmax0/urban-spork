package com.urbanspork.client;

import com.urbanspork.common.channel.DefaultChannelInboundHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;

public abstract class AbstractClientUdpOverTcpHandler<K> extends AbstractClientUdpRelayHandler<K> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClientUdpOverTcpHandler.class);
    private final EventLoopGroup workerGroup;

    protected AbstractClientUdpOverTcpHandler(ServerConfig config, Duration keepAlive, EventLoopGroup workerGroup) {
        super(config, keepAlive);
        this.workerGroup = workerGroup;
    }

    protected abstract Object convertToWrite(DatagramPacketWrapper msg);

    protected abstract K getKey(DatagramPacketWrapper msg);

    protected abstract ChannelInitializer<Channel> newOutboundInitializer(K key);

    protected abstract ChannelHandler newInboundHandler(Channel inboundChannel, K key);

    @Override
    protected Channel newBindingChannel(Channel inboundChannel, K key) {
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
        return new Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .handler(newOutboundInitializer(key))
            .connect(serverAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel outbound = future.channel();
                    outbound.pipeline().addLast(newInboundHandler(inboundChannel, key)); // R → L
                    inboundChannel.pipeline().addLast(new DefaultChannelInboundHandler(outbound)); // L → R
                } else {
                    logger.error("Connect relay server {} failed", serverAddress);
                }
            }).syncUninterruptibly().channel();
    }
}
