package com.urbanspork.test.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.protocol.socks.ClientHandshake;
import com.urbanspork.test.server.tcp.HttpTestServer;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.string.StringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class Socks5TCPTestClient {

    private static final Logger logger = LoggerFactory.getLogger(Socks5TCPTestClient.class);
    private static Channel channel;
    private static final EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        int proxyPort = 1089;
        String hostname;
        try {
            ClientConfig config = ConfigHandler.DEFAULT.read();
            proxyPort = config.getPort();
            hostname = config.getCurrent().getHost();
        } catch (Exception ignore) {
            hostname = InetAddress.getLoopbackAddress().getHostName();
        }
        InetSocketAddress proxyAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), proxyPort);
        InetSocketAddress dstAddress = new InetSocketAddress(hostname, HttpTestServer.PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        logger.info("Enter text (quit to end)");
        for (; ; ) {
            String line = in.readLine();
            if (line == null || "quit".equalsIgnoreCase(line)) {
                break;
            }
            connect(proxyAddress, dstAddress);
            byte[] bytes = line.getBytes();
            channel.writeAndFlush(Unpooled.wrappedBuffer(bytes));
            logger.info("Send msg");
        }
        bossGroup.shutdownGracefully();
    }

    private static void connect(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) throws InterruptedException, ExecutionException {
        if (channel == null) {
            ClientHandshake.Result result = ClientHandshake.noAuth(bossGroup, Socks5CommandType.CONNECT, proxyAddress, dstAddress).await().get();
            channel = result.sessionChannel();
            logger.info("Launch socks5 tcp test client => {}", channel.localAddress());
            channel.pipeline().addLast(
                new StringDecoder(),
                new SimpleChannelInboundHandler<String>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                        logger.info("↓ Receive msg ↓\n{}", msg);
                    }

                    @Override
                    public void channelUnregistered(ChannelHandlerContext ctx) {
                        logger.info("Channel unregistered");
                        channel = null;
                    }
                }
            );
        }
    }
}
