package com.urbanspork.test.server.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class HttpTestServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpTestServer.class);
    public static final int PORT = 16802;

    public static void main(String[] args) {
        launch(PORT, new DefaultPromise<>() {});
    }

    public static void launch(int port, Promise<Channel> promise) {
        try (EventLoopGroup bossGroup = new NioEventLoopGroup();
             EventLoopGroup workerGroup = new NioEventLoopGroup()) {
            new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                logger.debug("↓ Receive msg ↓\n{}", msg.toString(StandardCharsets.UTF_8));
                                ByteBuf res = Unpooled.buffer();
                                res.writeBytes("HTTP/1.1 200 OK\r\nServer: HttpTestServer\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n".getBytes());
                                res.writeBytes(("<h1>" + System.currentTimeMillis() + "</h1>\r\n").getBytes());
                                ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
                            }
                        });
                    }
                })
                .bind(port).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        Channel channel = future.channel();
                        logger.info("Launch echo test server => {}", channel.localAddress());
                        promise.setSuccess(channel);
                    } else {
                        promise.setFailure(future.cause());
                    }
                }).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
