package com.urbanspork.client;

import com.urbanspork.client.shadowsocks.ClientUDPReplayHandler;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.socks.Socks5DatagramPacketDecoder;
import com.urbanspork.common.protocol.socks.Socks5DatagramPacketEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void launch(ClientConfig clientConfig) {
        logger.info("Launching client => {}", clientConfig);
        int port = clientConfig.getPort();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerConfig config = clientConfig.getCurrent();
        try {
            if (Protocols.shadowsocks == config.getProtocol()) {
                new Bootstrap().group(bossGroup).channel(NioDatagramChannel.class)
                        .handler(new ChannelInitializer<>() {
                            @Override
                            protected void initChannel(Channel ch) {
                                ch.pipeline().addLast(
                                        new Socks5DatagramPacketEncoder(),
                                        new Socks5DatagramPacketDecoder(),
                                        new ClientUDPReplayHandler(config)
                                );
                            }
                        })
                        .bind(port).sync();
            }
            new ServerBootstrap().group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // socks5 require
                    .childOption(ChannelOption.TCP_NODELAY, false)
                    .childOption(ChannelOption.SO_LINGER, 1)
                    .childHandler(new ClientSocksInitializer(config, port))
                    .bind(port).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            Thread.currentThread().interrupt();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        launch(ConfigHandler.read(ClientConfig.class));
    }

}
