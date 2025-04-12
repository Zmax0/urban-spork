package com.urbanspork.client;

import com.urbanspork.client.shadowsocks.ClientUdpRelayHandler;
import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        launch(ConfigHandler.DEFAULT.read(), new CompletableFuture<>());
    }

    public static void launch(ClientConfig config, CompletableFuture<Instance> promise) {
        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        GlobalChannelTrafficShapingHandler traffic = new GlobalChannelTrafficShapingHandler(workerGroup);
        ClientInitializationContext context = new ClientInitializationContext(config, traffic);
        String host = config.getHost() == null ? InetAddress.getLoopbackAddress().getHostName() : config.getHost();
        try {
            ServerSocketChannel tcp = (ServerSocketChannel) new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true) // socks5 require
                .childOption(ChannelOption.TCP_NODELAY, false)
                .childOption(ChannelOption.SO_LINGER, 1)
                .childHandler(new ClientInitializer(context))
                .bind(host, config.getPort()).sync().channel();
            InetSocketAddress tcpLocalAddress = tcp.localAddress();
            int localPort = tcpLocalAddress.getPort();
            config.setPort(localPort);
            DatagramChannel udp = launchUdp(bossGroup, workerGroup, context);
            logger.info("Launch client => tcp{} udp{} ", tcpLocalAddress, udp.localAddress());
            Instance client = new Instance(tcp, udp, traffic.trafficCounter());
            promise.complete(client);
            CompletableFuture.allOf(
                CompletableFuture.supplyAsync(() -> client.tcp().closeFuture().syncUninterruptibly()),
                CompletableFuture.supplyAsync(() -> client.udp().closeFuture().syncUninterruptibly())
            ).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Launch client failed {}:{}", host, config.getPort(), e);
            promise.completeExceptionally(e);
        } finally {
            traffic.trafficCounter().stop();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static DatagramChannel launchUdp(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ClientInitializationContext context) throws InterruptedException {
        ClientConfig config = context.config();
        ChannelHandler udpTransportHandler = getUdpReplayHandler(workerGroup, config.getCurrent());
        String host = config.getHost() == null ? InetAddress.getLoopbackAddress().getHostName() : config.getHost();
        return (DatagramChannel) new Bootstrap().group(bossGroup).channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        context.traffic(),
                        new DatagramPacketEncoder(),
                        new DatagramPacketDecoder(),
                        udpTransportHandler
                    );
                }
            })
            .bind(host, config.getPort()).sync().channel();
    }

    private static ChannelHandler getUdpReplayHandler(EventLoopGroup workerGroup, ServerConfig current) {
        if (Protocol.vmess == current.getProtocol()) {
            if (current.quicEnabled()) {
                return new com.urbanspork.client.vmess.ClientUdpOverQuicHandler(current, workerGroup);
            }
            return new com.urbanspork.client.vmess.ClientUdpOverTcpHandler(current, workerGroup);
        } else if (Protocol.trojan == current.getProtocol()) {
            if (current.quicEnabled()) {
                return new com.urbanspork.client.trojan.ClientUdpOverQuicHandler(current, workerGroup);
            }
            return new com.urbanspork.client.trojan.ClientUdpOverTcpHandler(current, workerGroup);
        } else {
            return new ClientUdpRelayHandler(current, workerGroup);
        }
    }

    public record Instance(ServerSocketChannel tcp, DatagramChannel udp, TrafficCounter traffic) implements Closeable {
        @Override
        public void close() {
            tcp.close().syncUninterruptibly();
            udp.close().syncUninterruptibly();
        }
    }
}
