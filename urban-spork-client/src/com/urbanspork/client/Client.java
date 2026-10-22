package com.urbanspork.client;

import com.urbanspork.common.Runtime;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    /*
     * Whoever starts it owns it and closes it.
     */

    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("Client-Launcher");
        thread.setDaemon(true);
        return thread;
    });

    static void main() {
        Runtime runtime = new Runtime();
        try (Instance client = launch(runtime, ConfigHandler.DEFAULT.read()).join()) {
            client.awaitTermination();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } finally {
            runtime.close();
        }
    }

    public static CompletableFuture<Instance> launch(Runtime runtime, ClientConfig config) {
        CompletableFuture<Instance> promise = new CompletableFuture<>();
        POOL.execute(() -> {
            String host = config.getHost() == null ? InetAddress.getLoopbackAddress().getHostName() : config.getHost();
            int port = config.getPort();
            GlobalChannelTrafficShapingHandler traffic = null;
            ServerSocketChannel tcp = null;
            DatagramChannel udp = null;
            try {
                traffic = new GlobalChannelTrafficShapingHandler(runtime.childGroup());
                ConcurrentHashMap<String, ClientChannelTrafficHandler> channelTraffic = new ConcurrentHashMap<>();
                ClientChannelContext context = new ClientChannelContext(config.getCurrent(), traffic, channelTraffic);
                ClientTcpInitializer tcpInitializer = new ClientTcpInitializer(context);
                tcp = launchTcp(runtime.parentGroup(), runtime.childGroup(), host, port, tcpInitializer);
                int localPort = tcp.localAddress().getPort();
                udp = launchUdp(runtime.parentGroup(), host, localPort, new ClientUdpInitializer(context, runtime.childGroup()));
                config.setPort(localPort);
                InetSocketAddress tcpLocalAddress = tcp.localAddress();
                Instance client = new Instance(tcp, udp, traffic.trafficCounter(), channelTraffic, context, tcpInitializer);
                int clientId = System.identityHashCode(client);
                logger.info("Launch client [id:{}] => tcp{} udp{} ", clientId, tcpLocalAddress, udp.localAddress());
                promise.complete(client);
            } catch (InterruptedException e) {
                logger.warn("Interrupt main launch thread");
                close0(tcp, udp);
                release(traffic);
                Thread.currentThread().interrupt();
                promise.completeExceptionally(e);
            } catch (Throwable e) {
                logger.error("Launch client failed {}:{}", host, port, e);
                close0(tcp, udp);
                release(traffic);
                promise.completeExceptionally(e);
            }
        });
        return promise;
    }

    private static void release(GlobalChannelTrafficShapingHandler traffic) {
        if (traffic != null) {
            traffic.release();
        }
    }

    public static CompletableFuture<Instance> reload(Runtime runtime, Instance instance, ServerConfig config) {
        CompletableFuture<Client.Instance> promise = new CompletableFuture<>();
        instance.udp.eventLoop().execute(() -> {
            try {
                ClientChannelContext context = new ClientChannelContext(config, instance.context.traffic(), instance.context.channelTraffic());
                ClientUdpInitializer.replace(instance.udp, runtime.childGroup(), context);
                instance.tcpInitializer.reloadContext(context);
                Instance client = new Instance(instance.tcp, instance.udp, instance.traffic, instance.channelTraffic, context, instance.tcpInitializer);
                int clientId = System.identityHashCode(client);
                logger.info("Reload client [id:{}] => tcp{} udp{} ", clientId, instance.tcp.localAddress(), instance.udp.localAddress());
                promise.complete(client);
            } catch (Throwable e) {
                logger.error("Reload client failed ~> {}", config, e);
                promise.completeExceptionally(e);
            }
        });
        return promise;
    }

    private static ServerSocketChannel launchTcp(EventLoopGroup bossGroup, EventLoopGroup workerGroup, String host, int port, ClientTcpInitializer initializer) throws InterruptedException {
        return (ServerSocketChannel) new ServerBootstrap().group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.SO_KEEPALIVE, true) // socks5 require
            .childOption(ChannelOption.TCP_NODELAY, false)
            .childOption(ChannelOption.SO_LINGER, 1)
            .childHandler(initializer)
            .bind(host, port).sync().channel();
    }

    private static DatagramChannel launchUdp(EventLoopGroup bossGroup, String host, int port, ClientUdpInitializer initializer) throws InterruptedException {
        return (DatagramChannel) new Bootstrap().group(bossGroup).channel(NioDatagramChannel.class)
            .handler(initializer)
            .bind(host, port).sync().channel();
    }

    static void close0(ServerSocketChannel tcp, DatagramChannel udp) {
        ChannelFuture closeTcp = null;
        if (tcp != null) {
            closeTcp = tcp.close();
        }
        if (udp != null) {
            udp.close().syncUninterruptibly();
            udp.deregister().syncUninterruptibly();
        }
        if (closeTcp != null) {
            closeTcp.syncUninterruptibly();
            tcp.deregister().syncUninterruptibly();
        }
    }

    public record Instance(ServerSocketChannel tcp, DatagramChannel udp,
                           TrafficCounter traffic, Map<String, ClientChannelTrafficHandler> channelTraffic,
                           ClientChannelContext context, ClientTcpInitializer tcpInitializer) implements Closeable {

        public void awaitTermination() throws InterruptedException {
            tcp.closeFuture().sync();
            udp.closeFuture().sync();
        }

        @Override
        public void close() {
            close0(tcp, udp);
            context.traffic().release();
        }
    }
}
