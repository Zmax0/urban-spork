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
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        launch(configs);
    }

    private static void launch(List<ServerConfig> configs) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ExecutorService pool = Executors.newFixedThreadPool(configs.size());
        configs.forEach(config -> pool.submit(() -> {
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
                    .childHandler(new ServerInitializer(config));
                ChannelFuture f = b.bind(port).sync();
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                logger.error("Server listening thread is interrupted", e);
                Thread.currentThread().interrupt();
            } finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }, "UrbanSporkServer-" + config.getPort()));
        pool.shutdown();
    }

}
