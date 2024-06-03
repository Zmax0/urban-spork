package com.urbanspork.client;

import com.urbanspork.client.shadowsocks.ClientUdpRelayHandler;
import com.urbanspork.client.vmess.ClientUdpOverTCPHandler;
import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        launch(ConfigHandler.DEFAULT.read(), new CompletableFuture<>());
    }

    public static void launch(ClientConfig config, CompletableFuture<Instance> promise) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        GlobalChannelTrafficShapingHandler trafficShapingHandler = new GlobalChannelTrafficShapingHandler(bossGroup);
        ServerConfig current = config.getCurrent();
        current.setTrafficShapingHandler(trafficShapingHandler);
        try {
            new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true) // socks5 require
                .childOption(ChannelOption.TCP_NODELAY, false)
                .childOption(ChannelOption.SO_LINGER, 1)
                .childHandler(new ClientSocksInitializer(current))
                .bind(InetAddress.getLoopbackAddress(), config.getPort()).sync().addListener((ChannelFutureListener) future -> {
                    ServerSocketChannel tcp = (ServerSocketChannel) future.channel();
                    InetSocketAddress tcpLocalAddress = tcp.localAddress();
                    int localPort = tcpLocalAddress.getPort();
                    config.setPort(localPort);
                    DatagramChannel udp = launchUdp(bossGroup, workerGroup, config);
                    logger.info("Launch client => tcp{} udp{} ", tcpLocalAddress, udp.localAddress());
                    Instance client = new Instance(tcp, udp, trafficShapingHandler.trafficCounter());
                    promise.complete(client);
                });
            Instance client = promise.get();
            CompletableFuture.allOf(
                CompletableFuture.supplyAsync(() -> client.tcp().closeFuture().syncUninterruptibly()),
                CompletableFuture.supplyAsync(() -> client.udp().closeFuture().syncUninterruptibly())
            ).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Launch client failed", e);
            promise.completeExceptionally(e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static DatagramChannel launchUdp(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ClientConfig config) throws InterruptedException {
        ServerConfig current = config.getCurrent();
        ChannelHandler udpTransportHandler;
        if (Protocol.vmess == current.getProtocol()) {
            udpTransportHandler = new ClientUdpOverTCPHandler(current, workerGroup);
        } else {
            udpTransportHandler = new ClientUdpRelayHandler(current, workerGroup);
        }
        return (DatagramChannel) new Bootstrap().group(bossGroup).channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        new DatagramPacketEncoder(),
                        new DatagramPacketDecoder(),
                        udpTransportHandler,
                        current.getTrafficShapingHandler()
                    );
                }
            })
            .bind(InetAddress.getLoopbackAddress(), config.getPort()).sync().channel();
    }

    public record Instance(ServerSocketChannel tcp, DatagramChannel udp, TrafficCounter traffic) {
        public void close() {
            traffic.stop();
            tcp.close().awaitUninterruptibly();
            udp.close().awaitUninterruptibly();
        }
    }
}
