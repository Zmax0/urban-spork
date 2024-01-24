package com.urbanspork.server;

import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.udp.UDPReplayCodec;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocols;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        launch(ConfigHandler.DEFAULT.read().getServers());
    }

    public static void launch(List<ServerConfig> configs) {
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("Server config in the file is empty");
        }
        configs = configs.stream().filter(config -> config.getHost().matches("localhost|127.*|[:1]|0.0.0.0|[:0]")).toList();
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("None available server");
        }
        launch(configs, new CompletableFuture<>());
    }

    public static void launch(List<ServerConfig> configs, CompletableFuture<List<Map.Entry<ServerSocketChannel, Optional<DatagramChannel>>>> promise) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        List<CompletableFuture<ChannelFuture>> futures = new ArrayList<>();
        try {
            List<Map.Entry<ServerSocketChannel, Optional<DatagramChannel>>> servers = new ArrayList<>(configs.size());
            for (ServerConfig config : configs) {
                CompletableFuture<Map.Entry<ServerSocketChannel, Optional<DatagramChannel>>> innerPromise = new CompletableFuture<>();
                startup(bossGroup, workerGroup, config, innerPromise);
                servers.add(innerPromise.get());
            }
            promise.complete(servers);
            for (Map.Entry<ServerSocketChannel, Optional<DatagramChannel>> server : servers) {
                futures.add(CompletableFuture.supplyAsync(() -> server.getKey().closeFuture().syncUninterruptibly()));
                server.getValue().map(v -> CompletableFuture.supplyAsync(() -> v.closeFuture().syncUninterruptibly())).ifPresent(futures::add);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{})).get();
        } catch (InterruptedException e) {
            logger.warn("Interrupt main launch thread");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Startup server failed", e);
            promise.completeExceptionally(e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void close(List<Map.Entry<ServerSocketChannel, Optional<DatagramChannel>>> servers) {
        for (Map.Entry<ServerSocketChannel, Optional<DatagramChannel>> entry : servers) {
            entry.getKey().close().awaitUninterruptibly();
            entry.getValue().ifPresent(c -> c.close().awaitUninterruptibly());
        }
    }

    private static void startup(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerConfig config, CompletableFuture<Map.Entry<ServerSocketChannel, Optional<DatagramChannel>>> promise)
        throws InterruptedException {
        if (Protocols.shadowsocks == config.getProtocol()) {
            List<ServerUserConfig> user = config.getUser();
            if (user != null) {
                user.stream().map(ServerUser::from).forEach(ServerUserManager.DEFAULT::addUser);
            }
        }
        new ServerBootstrap().group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.SO_LINGER, 1)
            .childHandler(new ServerInitializer(config))
            .bind(config.getPort()).sync().addListener((ChannelFutureListener) future -> {
                ServerSocketChannel tcp = (ServerSocketChannel) future.channel();
                InetSocketAddress tcpLocalAddress = tcp.localAddress();
                config.setPort(tcpLocalAddress.getPort());
                logger.info("Startup tcp server => {}", config);
                Optional<DatagramChannel> udp = startupUdp(bossGroup, workerGroup, config);
                promise.complete(Map.entry(tcp, udp));
            });
    }

    private static Optional<DatagramChannel> startupUdp(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerConfig config) throws InterruptedException {
        if (Protocols.shadowsocks == config.getProtocol() && config.udpEnabled()) {
            return Optional.of((DatagramChannel) new Bootstrap().group(bossGroup).channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                            new UDPReplayCodec(config, Mode.Server),
                            new ServerUDPReplayHandler(config.getPacketEncoding(), workerGroup),
                            new ExceptionHandler(config)
                        );
                    }
                })
                .bind(config.getPort()).sync().addListener(future -> logger.info("Startup upd server => {}", config)).channel());
        } else {
            return Optional.empty();
        }
    }
}
