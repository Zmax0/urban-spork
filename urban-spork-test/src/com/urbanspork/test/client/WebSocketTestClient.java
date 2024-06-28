package com.urbanspork.test.client;

import com.urbanspork.test.server.tcp.EchoWebSocketTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class WebSocketTestClient {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketTestClient.class);

    public static void main(String[] args) throws URISyntaxException, IOException {
        URI uri = new URI("ws", null, InetAddress.getLoopbackAddress().getHostName(), EchoWebSocketTestServer.PORT, EchoWebSocketTestServer.PATH, null, null);
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.HOST, "www.example.com");
        WebSocketClientProtocolConfig config = WebSocketClientProtocolConfig.newBuilder()
            .generateOriginHeader(false)
            .customHeaders(headers)
            .webSocketUri(uri)
            .build();
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Channel channel = new Bootstrap().group(group).channel(NioSocketChannel.class).handler(
                new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                            new HttpClientCodec(),
                            new HttpObjectAggregator(0xffff),
                            new WebSocketClientProtocolHandler(config),
                            new SimpleChannelInboundHandler<WebSocketFrame>(false) {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
                                    logger.info("Receive server msg {}", msg.content().toString(StandardCharsets.UTF_8));
                                }
                            }
                        );
                    }
                }
            ).connect(uri.getHost(), uri.getPort()).sync().channel();
            logger.info("Connecting {} by {}", uri, channel.localAddress());
            sendMsg(channel);
            logger.info("Sending close frame");
            channel.writeAndFlush(new CloseWebSocketFrame()).sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static void sendMsg(Channel channel) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        logger.info("Enter text (quit to end)");
        for (; ; ) {
            String line = in.readLine();
            if (line == null || "quit".equalsIgnoreCase(line)) {
                break;
            }
            logger.info("Send msg {}", line);
            channel.writeAndFlush(new TextWebSocketFrame(line));
        }
    }
}
