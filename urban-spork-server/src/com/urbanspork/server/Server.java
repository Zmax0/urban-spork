package com.urbanspork.server;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class Server {

    public static void main(String[] args) throws IOException {
        List<ServerConfig> serverConfigs = Optional
            .ofNullable(ConfigHandler.read(ClientConfig.class))
            .orElseThrow(() -> new IllegalArgumentException("Please put the 'config.json' file into the folder"))
            .getServers();
        if (serverConfigs.isEmpty()) {
            throw new IllegalArgumentException("Server config in the file is empty");
        }
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        serverConfigs.forEach(serverConfig -> new Thread(() -> {
            try {
                int port = Integer.parseInt(serverConfig.getPort());
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ServerInitializer(serverConfig));
                ChannelFuture f = b.bind(port).sync();
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                // skip
            } finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }, "UrbanSporkServer-" + serverConfig.getPort()).start());
    }

}
