package com.urbanspork.server;

import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.udp.UdpRelayCodec;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
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
        launch(configs, new CompletableFuture<>());
    }

    public static void launch(List<ServerConfig> configs, CompletableFuture<List<Instance>> promise) {
        Context context = Context.newCheckReplayInstance();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            List<Instance> servers = new ArrayList<>(configs.size());
            int count = 0;
            for (ServerConfig config : configs) {
                Instance server = startup(bossGroup, workerGroup, new ServerInitializationContext(config, context));
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
            context.release();
        }
    }

    private static Instance startup(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerInitializationContext context)
        throws InterruptedException {
        ServerConfig config = context.config();
        ServerSocketChannel tcp = (ServerSocketChannel) new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ServerInitializer(context))
            .bind(config.getPort())
            .sync().addListener(future -> logger.info("Startup tcp server => {}", config))
            .channel().closeFuture().channel();
        config.setPort(tcp.localAddress().getPort());
        Optional<DatagramChannel> udp = startupUdp(bossGroup, workerGroup, context);
        return new Instance(tcp, udp);
    }

    private static Optional<DatagramChannel> startupUdp(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerInitializationContext context) throws InterruptedException {
        ServerConfig config = context.config();
        if (Protocol.shadowsocks == config.getProtocol() && config.udpEnabled()) {
            Channel channel = new Bootstrap().group(bossGroup).channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                            new UdpRelayCodec(config, Mode.Server, context.userManager()),
                            new ServerUdpRelayHandler(config.getPacketEncoding(), workerGroup),
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

    public record Instance(ServerSocketChannel tcp, Optional<DatagramChannel> udp) implements Closeable {
        @Override
        public void close() {
            udp.ifPresent(c -> c.close().awaitUninterruptibly());
            tcp.close().awaitUninterruptibly();
        }
    }
}
