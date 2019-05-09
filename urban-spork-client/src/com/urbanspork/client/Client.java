package com.urbanspork.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.config.ClientConfig;
import com.urbanspork.config.ConfigHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void launch(ClientConfig clientConfig) throws Exception {
        logger.info("Proxy client launched ~> {}", clientConfig);
        int port = Integer.valueOf(clientConfig.getPort());
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ClientInitializer(clientConfig)); 
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        launch(ConfigHandler.read(ClientConfig.class));
    }

}
