package com.urbanspork.server;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.UDPReplayCodec;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.network.Direction;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
        launch(configs, new DefaultPromise<>() {});
    }

    public static void launch(List<ServerConfig> configs, Promise<List<ServerSocketChannel>> promise) {
        int size = configs.size();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        DefaultEventLoop executor = new DefaultEventLoop();
        try {
            List<ServerSocketChannel> result = new ArrayList<>(size);
            for (ServerConfig config : configs) {
                Promise<ServerSocketChannel> innerPromise = executor.newPromise();
                startup(bossGroup, workerGroup, config, innerPromise);
                innerPromise.await(5, TimeUnit.SECONDS);
                if (innerPromise.isSuccess()) {
                    result.add(innerPromise.get());
                } else {
                    promise.setFailure(innerPromise.cause());
                    return;
                }
            }
            promise.setSuccess(result);
            result.getLast().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("Interrupt main launch thread");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            promise.setFailure(e);
        } finally {
            executor.shutdownGracefully();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static void startup(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerConfig config, Promise<ServerSocketChannel> promise) {
        try {
            int port = config.getPort();
            if (Protocols.shadowsocks == config.getProtocol() && config.udpEnabled()) {
                new Bootstrap().group(bossGroup).channel(NioDatagramChannel.class)
                    .attr(AttributeKeys.DIRECTION, Direction.Inbound)
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
                    .bind(port).sync().addListener(future -> logger.info("Startup udp server => {}", config));
            }
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .attr(AttributeKeys.DIRECTION, Direction.Inbound)
                .childOption(ChannelOption.SO_LINGER, 1)
                .childHandler(new ServerInitializer(config))
                .bind(port).sync().addListener((ChannelFutureListener) future -> {
                    Channel channel = future.channel();
                    logger.info("Startup tcp server => {}", config);
                    promise.setSuccess((ServerSocketChannel) channel);
                });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Startup server failed", e);
            promise.setFailure(e);
        }
    }
}
