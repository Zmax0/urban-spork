package com.urbanspork.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void launch(ClientConfig clientConfig) {
        logger.info("Launching proxy client ~> {}", clientConfig);
        int port = Integer.parseInt(clientConfig.getPort());
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            new ServerBootstrap().group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ClientChannelInitializer(clientConfig))
                    .bind(port).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.info("Interrupt thread [{}]", Thread.currentThread().getName());
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        launch(ConfigHandler.read(ClientConfig.class));
    }

}
