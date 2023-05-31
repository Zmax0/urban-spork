package com.urbanspork.server;

import com.urbanspork.common.codec.shadowsocks.ShadowsocksUDPReplayCodec;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.server.shadowsocks.ServerUDPReplayHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        List<ServerConfig> configs = ConfigHandler.DEFAULT.read().getServers();
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("Server config in the file is empty");
        }
        configs = configs.stream()
            .filter(config -> config.getHost().matches("localhost|127.*|[:1]|0.0.0.0|[:0]"))
            .filter(config -> Protocols.shadowsocks == config.getProtocol())
            .toList();
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("None available shadowsocks server");
        }
        launch(configs, new DefaultPromise<>() {});
    }

    public static void launch(List<ServerConfig> configs, Promise<List<ServerSocketChannel>> promise) {
        int size = configs.size();
        ExecutorService pool = Executors.newFixedThreadPool(size, new WorkingThreadFactory());
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        DefaultEventLoop executor = new DefaultEventLoop();
        try {
            List<ServerSocketChannel> result = new ArrayList<>(size);
            for (ServerConfig config : configs) {
                DefaultPromise<ServerSocketChannel> innerPromise = new DefaultPromise<>(executor);
                pool.submit(() -> startup(bossGroup, workerGroup, config, innerPromise));
                result.add(innerPromise.await().get());
            }
            promise.setSuccess(result);
            executor.shutdownGracefully();
            pool.shutdown();
            boolean terminated = false;
            while (!terminated) {
                terminated = pool.awaitTermination(1, TimeUnit.HOURS);
            }
        } catch (InterruptedException | ExecutionException e) {
            promise.setFailure(e);
            pool.shutdownNow();
            if (e instanceof InterruptedException) {
                logger.error("Launch thread is interrupted");
                Thread.currentThread().interrupt();
            }
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static void startup(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerConfig config, Promise<ServerSocketChannel> promise) {
        try {
            int port = config.getPort();
            if (config.udpEnabled()) {
                new Bootstrap().group(bossGroup)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(
                                new ShadowsocksUDPReplayCodec(config),
                                new ServerUDPReplayHandler(config, workerGroup)
                            );
                        }
                    })
                    .bind(port).sync();
            }
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ServerInitializer(config))
                .bind(port).sync().addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        Channel channel = f.channel();
                        logger.info("Startup server => {}", channel);
                        promise.setSuccess((ServerSocketChannel) channel);
                    } else {
                        Throwable cause = f.cause();
                        logger.error("Startup server failed", cause);
                        promise.setFailure(cause);
                    }
                }).channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class WorkingThreadFactory implements ThreadFactory {

        private final AtomicInteger count = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("urban-spork-" + count.incrementAndGet());
            return thread;
        }
    }
}
