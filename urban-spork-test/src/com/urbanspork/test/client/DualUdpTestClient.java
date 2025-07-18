package com.urbanspork.test.client;

import com.urbanspork.test.server.udp.DelayedEchoTestServer;
import com.urbanspork.test.server.udp.SimpleEchoTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class DualUdpTestClient {

    private static final Logger logger = LoggerFactory.getLogger(DualUdpTestClient.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
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
        InetSocketAddress dstAddress1 = new InetSocketAddress(InetAddress.getLoopbackAddress(), SimpleEchoTestServer.PORT);
        InetSocketAddress dstAddress2 = new InetSocketAddress(InetAddress.getLoopbackAddress(), DelayedEchoTestServer.PORT);
        sendMsg(channel, dstAddress1, dstAddress2);
        bossGroup.shutdownGracefully();
    }

    private static void sendMsg(Channel channel, InetSocketAddress... dstAddress) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        logger.info("Enter text (quit to end)");
        for (; ; ) {
            String line = in.readLine();
            if (line == null || "quit".equalsIgnoreCase(line)) {
                break;
            }
            for (InetSocketAddress address : dstAddress) {
                DatagramPacket msg = new DatagramPacket(Unpooled.copiedBuffer(line.getBytes()), address);
                logger.info("Send msg {}", msg);
                channel.writeAndFlush(msg);
            }
        }
    }
}
