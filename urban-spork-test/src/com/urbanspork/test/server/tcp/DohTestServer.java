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
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public class DohTestServer {
    public static final int PORT = 16803;
    private static final Logger logger = LoggerFactory.getLogger(DohTestServer.class);

    public static void main(String[] args) {
        launch(PORT, new DefaultPromise<>() {});
    }

    public static void launch(int port, Promise<ServerSocketChannel> promise) {
        URL crt = Objects.requireNonNull(DohTestServer.class.getResource("/localhost.crt"));
        URL key = Objects.requireNonNull(DohTestServer.class.getResource("/localhost.key"));
        try (EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
             EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())) {
            new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws SSLException, URISyntaxException {
                        SslContext sslContext = SslContextBuilder.forServer(new File(crt.toURI()), new File(key.toURI()), null).build();
                        String serverName = "localhost";
                        ch.pipeline().addLast(
                            sslContext.newHandler(ch.alloc(), serverName, 16803),
                            new HttpServerCodec(),
                            new HttpObjectAggregator(0xffff),
                            new SimpleChannelInboundHandler<FullHttpRequest>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
                                    QueryStringDecoder queryString = new QueryStringDecoder(request.uri());
                                    String name = Optional.ofNullable(queryString.parameters().get("name")).orElse(Collections.emptyList()).stream().findFirst().orElse("localhost");
                                    String resolved = Optional.ofNullable(queryString.parameters().get("resolved")).orElse(Collections.emptyList()).stream().findFirst().orElse("127.0.0.1");
                                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                                    response.headers().set(HttpHeaderNames.SERVER, "DohTestServer").set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
                                    String json = String.format(
                                        """
                                            {
                                                "Status": 0,
                                                "TC": false,
                                                "RD": true,
                                                "RA": true,
                                                "AD": false,
                                                "CD": false,
                                                "Answer": [
                                                    {
                                                        "name": "%s.",
                                                        "TTL": 1,
                                                        "type": 1,
                                                        "data": "%s"
                                                    }
                                                ]
                                            }
                                            """, name, resolved
                                    );
                                    response.content().writeCharSequence(json, StandardCharsets.UTF_8);
                                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                                }
                            }
                        );
                    }
                })
                .bind(port).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        ServerSocketChannel channel = (ServerSocketChannel) future.channel();
                        logger.info("Launch echo test server => {}", channel.localAddress());
                        promise.setSuccess(channel);
                    } else {
                        promise.setFailure(future.cause());
                    }
                }).sync().channel().closeFuture().sync();
            logger.info("Doh test server close");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
