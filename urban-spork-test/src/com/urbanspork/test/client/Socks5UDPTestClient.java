package com.urbanspork.test.client;

import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.protocol.socks.ClientHandshake;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.test.server.udp.DelayedEchoTestServer;
import com.urbanspork.test.server.udp.SimpleEchoTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class Socks5UDPTestClient {

    private static final Logger logger = LoggerFactory.getLogger(Socks5UDPTestClient.class);

    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {
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
        InetSocketAddress dstAddress1 = new InetSocketAddress(hostname, SimpleEchoTestServer.PORT);
        InetSocketAddress dstAddress2 = new InetSocketAddress(hostname, DelayedEchoTestServer.PORT);
        EventLoopGroup group = new NioEventLoopGroup();
        ClientHandshake.Result result1 = ClientHandshake.noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress1).await().get();
        ClientHandshake.Result result2 = ClientHandshake.noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress2).await().get();
        Socks5CommandResponse response1 = result1.response();
        Socks5CommandResponse response2 = result2.response();
        logger.info("Associate ports: [{}, {}]", response1.bndPort(), response2.bndPort());
        Channel channel = new Bootstrap().group(group)
            .channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        new DatagramPacketEncoder(),
                        new DatagramPacketDecoder(),
                        new SimpleChannelInboundHandler<DatagramPacketWrapper>(false) {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacketWrapper msg) {
                                ByteBuf content = msg.packet().content();
                                InetSocketAddress dst = msg.proxy();
                                logger.info("Receive msg {} - {}", dst, content.readCharSequence(content.readableBytes(), StandardCharsets.UTF_8));
                                if (!dstAddress1.equals(dst) && !dstAddress2.equals(dst)) {
                                    logger.error("Destination address is unexpected.");
                                }
                            }
                        }
                    );
                }
            })
            .bind(0).sync().channel();
        logger.info("Bind local address {}", channel.localAddress());
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        logger.info("Enter text (quit to end)");
        for (; ; ) {
            String line = in.readLine();
            if (line == null || "quit".equalsIgnoreCase(line)) {
                break;
            }
            byte[] bytes = line.getBytes();
            sendMsg(channel, dstAddress1, new InetSocketAddress(response1.bndAddr(), response1.bndPort()), bytes);
            sendMsg(channel, dstAddress2, new InetSocketAddress(response2.bndAddr(), response2.bndPort()), bytes);
        }
        result1.sessionChannel().eventLoop().shutdownGracefully();
        result2.sessionChannel().eventLoop().shutdownGracefully();
        group.shutdownGracefully();
    }

    private static void sendMsg(Channel channel, InetSocketAddress dstAddress, InetSocketAddress socksAddress, byte[] bytes) {
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(bytes), dstAddress);
        DatagramPacketWrapper msg = new DatagramPacketWrapper(data, socksAddress);
        logger.info("Send msg {}", msg);
        channel.writeAndFlush(msg);
    }
}
