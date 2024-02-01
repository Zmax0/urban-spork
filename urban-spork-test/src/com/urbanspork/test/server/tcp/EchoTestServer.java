package com.urbanspork.test.server.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class EchoTestServer {

    private static final Logger logger = LoggerFactory.getLogger(EchoTestServer.class);
    public static final int PORT = 16802;

    public static void main(String[] args) {
        launch(PORT, new CompletableFuture<>());
    }

    public static void launch(int port, CompletableFuture<ServerSocketChannel> promise) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>(false) {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                logger.info("Receive msg and send back");
                                ctx.writeAndFlush(msg);
                            }
                        });
                    }
                })
                .bind(port).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        ServerSocketChannel channel = (ServerSocketChannel) future.channel();
                        logger.info("Launch echo test server => {}", channel.localAddress());
                        promise.complete(channel);
                    } else {
                        promise.completeExceptionally(future.cause());
                    }
                }).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
