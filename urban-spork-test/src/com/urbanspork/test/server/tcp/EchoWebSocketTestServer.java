package com.urbanspork.test.server.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoWebSocketTestServer {
    private static final Logger logger = LoggerFactory.getLogger(EchoWebSocketTestServer.class);
    public static final int PORT = 16802;
    public static final String PATH = "/ws";

    static void main() {
        launch(PORT, new DefaultPromise<>() {});
    }

    public static void launch(int port, Promise<Channel> promise) {
        try (EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
             EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())) {
            new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                            new HttpServerCodec(),
                            new HttpObjectAggregator(0xffff),
                            new WebSocketServerProtocolHandler(PATH),
                            new SimpleChannelInboundHandler<WebSocketFrame>(false) {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
                                    ctx.writeAndFlush(new TextWebSocketFrame(msg.content()));
                                }
                            });
                    }
                })
                .bind(port).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        Channel channel = future.channel();
                        logger.info("Launch echo websocket test server => {}", channel.localAddress());
                        promise.setSuccess(channel);
                    } else {
                        promise.setFailure(future.cause());
                    }
                }).sync().channel().closeFuture().sync();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
