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
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class HttpPostMultipartTestServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpPostMultipartTestServer.class);
    public static final int PORT = 16802;

    public static void main(String[] args) {
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
                            new SimpleChannelInboundHandler<HttpRequest>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) {
                                    logger.info("↓ Receive msg ↓\n{}", msg);
                                    String contentType = msg.headers().get(HttpHeaderNames.CONTENT_TYPE, "");
                                    if (contentType.startsWith("multipart")) {
                                        HttpPostMultipartRequestDecoder multipartRequestDecoder = new HttpPostMultipartRequestDecoder(msg);
                                        ctx.pipeline().addLast(new MultipartHandler(multipartRequestDecoder));
                                    } else {
                                        response(ctx);
                                    }
                                }
                            }
                        );
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

    private static class MultipartHandler extends SimpleChannelInboundHandler<HttpContent> {
        private final HttpPostMultipartRequestDecoder multipartRequestDecoder;

        MultipartHandler(HttpPostMultipartRequestDecoder multipartRequestDecoder) {
            this.multipartRequestDecoder = multipartRequestDecoder;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpContent msg) {
            multipartRequestDecoder.offer(msg);
            if (multipartRequestDecoder.hasNext()) {
                multipartRequestDecoder.next();
                response(ctx);
            }
        }
    }

    private static void response(ChannelHandlerContext ctx) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.content().writeCharSequence("<h1>" + System.currentTimeMillis() + "</h1>\r\n", StandardCharsets.UTF_8);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}

