package com.urbanspork.server;

import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.udp.UdpRelayCodec;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

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

    public static void launch(List<ServerConfig> configs, CompletableFuture<List<Instance>> promise) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            List<Instance> servers = new ArrayList<>(configs.size());
            int count = 0;
            for (ServerConfig config : configs) {
                Instance server = startup(bossGroup, workerGroup, config);
                count += server.udp().isPresent() ? 2 : 1;
                servers.add(server);
            }
            CountDownLatch latch = new CountDownLatch(count);
            for (Instance server : servers) {
                server.tcp().closeFuture().addListener(future -> latch.countDown());
                server.udp().ifPresent(v -> v.closeFuture().addListener(future -> latch.countDown()));
            }
            promise.complete(servers);
            latch.await(); // main thread is waiting here
            logger.info("Server is terminated");
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

    private static Instance startup(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerConfig config)
        throws InterruptedException {
        if (Protocol.shadowsocks == config.getProtocol()) {
            List<ServerUserConfig> user = config.getUser();
            if (user != null) {
                user.stream().map(ServerUser::from).forEach(ServerUserManager.DEFAULT::addUser);
            }
        }
        Context context = Context.checkReplay();
        ServerSocketChannel tcp;
        try {
            tcp = (ServerSocketChannel) new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ServerInitializer(config, context))
                .childOption(ChannelOption.ALLOW_HALF_CLOSURE, true)
                .bind(config.getPort()).sync().addListener(future -> logger.info("Startup tcp server => {}", config)).channel()
                .closeFuture().addListener(future -> context.release()).channel();
        } catch (Exception e) {
            context.release();
            throw e;
        }
        config.setPort(tcp.localAddress().getPort());
        Optional<DatagramChannel> udp = startupUdp(bossGroup, workerGroup, config);
        return new Instance(tcp, udp);
    }

    private static Optional<DatagramChannel> startupUdp(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerConfig config) throws InterruptedException {
        if (Protocol.shadowsocks == config.getProtocol() && config.udpEnabled()) {
            Channel channel = new Bootstrap().group(bossGroup).channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                            new UdpRelayCodec(config, Mode.Server),
                            new ServerUDPRelayHandler(config.getPacketEncoding(), workerGroup),
                            new ExceptionHandler(config)
                        );
                    }
                })
                .bind(config.getPort()).sync().addListener(future -> logger.info("Startup upd server => {}", config)).channel();
            return Optional.of((DatagramChannel) channel);
        } else {
            return Optional.empty();
        }
    }

    public record Instance(ServerSocketChannel tcp, Optional<DatagramChannel> udp) {
        public void close() {
            tcp.close().awaitUninterruptibly();
            udp.ifPresent(c -> c.close().awaitUninterruptibly());
        }
    }
}
