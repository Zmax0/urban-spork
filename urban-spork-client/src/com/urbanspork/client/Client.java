package com.urbanspork.client;

import com.urbanspork.client.shadowsocks.ClientUDPReplayHandler;
import com.urbanspork.client.vmess.ClientUDPOverTCPHandler;
import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.config.ClientConfig;
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

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        launch(ConfigHandler.DEFAULT.read());
    }

    public static void launch(ClientConfig config) {
        launch(config, new DefaultPromise<>() {});
    }

    public static void launch(ClientConfig config, Promise<ServerSocketChannel> promise) {
        int port = config.getPort();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerConfig current = config.getCurrent();
        ChannelHandler udpTransportHandler;
        if (Protocols.vmess == current.getProtocol()) {
            udpTransportHandler = new ClientUDPOverTCPHandler(current, workerGroup);
        } else {
            udpTransportHandler = new ClientUDPReplayHandler(current, workerGroup);
        }
        try {
            new Bootstrap().group(bossGroup).channel(NioDatagramChannel.class)
                .attr(AttributeKeys.DIRECTION, Direction.Inbound)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                            new DatagramPacketEncoder(),
                            new DatagramPacketDecoder(),
                            udpTransportHandler,
                            new ExceptionHandler(current)
                        );
                    }
                })
                .bind(port).sync();
            new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true) // socks5 require
                .childOption(ChannelOption.TCP_NODELAY, false)
                .childOption(ChannelOption.SO_LINGER, 1)
                .childHandler(new ClientSocksInitializer(current, port))
                .bind(port).sync().addListener((ChannelFutureListener) future -> {
                    logger.info("Launch client => {} ", config);
                    promise.setSuccess((ServerSocketChannel) future.channel());
                }).channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Launch client failed", e);
            promise.setFailure(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
