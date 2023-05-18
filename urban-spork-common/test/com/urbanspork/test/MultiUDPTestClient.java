package com.urbanspork.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class MultiUDPTestClient {

    private static final Logger logger = LoggerFactory.getLogger(MultiUDPTestClient.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        String hostname = "localhost";
        InetSocketAddress dstAddress = new InetSocketAddress(hostname, SimpleUDPTestServer.PORT);
        InetSocketAddress dstAddress2 = new InetSocketAddress(hostname, DelayedUDPTestServer.PORT);
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        Channel channel = new Bootstrap().group(bossGroup)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                                new SimpleChannelInboundHandler<DatagramPacket>(false) {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
                                        ByteBuf content = msg.content();
                                        logger.info("Receive msg from {} -> {}", msg.sender(), content.readCharSequence(content.readableBytes(), StandardCharsets.UTF_8));
                                    }
                                }
                        );
                    }
                })
                .bind(0).sync().channel();
        logger.info("Bind local address {}", channel.localAddress());
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Enter text (quit to end)");
        for (; ; ) {
            String line = in.readLine();
            if (line == null || "quit".equalsIgnoreCase(line)) {
                break;
            }
            ByteBuf data = Unpooled.wrappedBuffer(line.getBytes());
            DatagramPacket msg = new DatagramPacket(data.retain(), dstAddress);
            DatagramPacket msg2 = new DatagramPacket(data.retain(), dstAddress2);
            logger.info("Send msg {}", msg);
            channel.writeAndFlush(msg);
            logger.info("Send msg {}", msg2);
            channel.writeAndFlush(msg2);
        }
        bossGroup.shutdownGracefully();
    }
}
