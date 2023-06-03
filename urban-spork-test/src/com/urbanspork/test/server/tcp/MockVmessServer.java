package com.urbanspork.test.server.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;

public class MockVmessServer {

    public static final int PORT = 16803;

    public static void main(String[] args) {
        launch(PORT, new DefaultPromise<>() {});
    }

    public static void launch(int port, Promise<Channel> promise) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                            new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                ctx.writeAndFlush(msg);
                            }
                        });
                    }
                })
                .bind(port).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        promise.setSuccess(future.channel());
                    } else {
                        promise.setFailure(future.cause());
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
