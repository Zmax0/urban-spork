package com.urbanspork.server;

import java.util.List;
import java.util.Objects;

import com.urbanspork.config.ClientConfig;
import com.urbanspork.config.ConfigHandler;
import com.urbanspork.config.ServerConfig;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Server {

    public static void main(String[] args) throws Exception {
        List<ServerConfig> serverConfigs = Objects.requireNonNull(ConfigHandler.read(ClientConfig.class), "Please put the 'config.json' file into the folder").getServers();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        for (ServerConfig serverConfig : serverConfigs) {
            try {
                int port = Integer.valueOf(serverConfig.getPort());
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ServerInitializer(serverConfig));
                ChannelFuture f = b.bind(port).sync();
                f.channel().closeFuture().sync();
            } finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }
    }

}
