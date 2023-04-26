package com.urbanspork.server;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.server.shadowsocks.ShadowsocksUDPChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws IOException {
        List<ServerConfig> configs = ConfigHandler.read(ClientConfig.class).getServers();
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("Server config in the file is empty");
        }
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        configs = configs.stream()
                .filter(config -> config.getHost().matches("localhost|127.*|[:1]|0.0.0.0|[:0]"))
                .filter(config -> Protocols.shadowsocks == config.getProtocol()).toList();
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("None available shadowsocks server");
        }
        ExecutorService threadPool = Executors.newFixedThreadPool(configs.size());
        configs.forEach(config -> threadPool.submit(() -> {
            try {
                int port = config.getPort();
                if (config.udpEnabled()) {
                    new Bootstrap().group(bossGroup)
                            .option(ChannelOption.SO_BROADCAST, true)
                            .channel(NioDatagramChannel.class)
                            .handler(new ShadowsocksUDPChannelInitializer(config))
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
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
                Thread.currentThread().interrupt();
            } finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }, "UrbanSporkServer-" + config.getPort()));
        threadPool.shutdown();
    }

}
