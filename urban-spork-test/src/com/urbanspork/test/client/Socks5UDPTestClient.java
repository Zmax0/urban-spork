package com.urbanspork.test.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.socks.DatagramPacketDecoder;
import com.urbanspork.common.protocol.socks.DatagramPacketEncoder;
import com.urbanspork.common.protocol.socks.Handshake;
import com.urbanspork.test.server.udp.DelayedEchoTestServer;
import com.urbanspork.test.server.udp.SimpleEchoTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class Socks5UDPTestClient {

    private static final Logger logger = LoggerFactory.getLogger(Socks5UDPTestClient.class);
    private static final String LOCALHOST = "localhost";

    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {
        int proxyPort = 1089;
        String hostname;
        try {
            ClientConfig config = ConfigHandler.DEFAULT.read();
            proxyPort = config.getPort();
            hostname = config.getCurrent().getHost();
        } catch (Exception ignore) {
            hostname = LOCALHOST;
        }
        InetSocketAddress proxyAddress = new InetSocketAddress(LOCALHOST, proxyPort);
        InetSocketAddress dstAddress1 = new InetSocketAddress(hostname, SimpleEchoTestServer.PORT);
        InetSocketAddress dstAddress2 = new InetSocketAddress(hostname, DelayedEchoTestServer.PORT);
        Handshake.Result result1 = Handshake.noAuth(Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress1).await().get();
        Handshake.Result result2 = Handshake.noAuth(Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress2).await().get();
        int bndPort1 = result1.response().bndPort();
        int bndPort2 = result2.response().bndPort();
        logger.info("Associate ports: [1:{}, 2:{}]", bndPort1, bndPort2);
        EventLoopGroup bossGroup = new NioEventLoopGroup(2);
        Channel channel = new Bootstrap().group(bossGroup)
            .channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        new DatagramPacketEncoder(),
                        new DatagramPacketDecoder(),
                        new SimpleChannelInboundHandler<TernaryDatagramPacket>(false) {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, TernaryDatagramPacket msg) {
                                ByteBuf content = msg.packet().content();
                                InetSocketAddress dst = msg.third();
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
        InetSocketAddress socksAddress1 = new InetSocketAddress(LOCALHOST, bndPort1);
        InetSocketAddress socksAddress2 = new InetSocketAddress(LOCALHOST, bndPort2);
        for (; ; ) {
            String line = in.readLine();
            if (line == null || "quit".equalsIgnoreCase(line)) {
                break;
            }
            byte[] bytes = line.getBytes();
            sendMsg(channel, dstAddress1, socksAddress1, bytes);
            sendMsg(channel, dstAddress2, socksAddress2, bytes);
        }
        result1.sessionChannel().eventLoop().shutdownGracefully();
        result2.sessionChannel().eventLoop().shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    private static void sendMsg(Channel channel, InetSocketAddress dstAddress, InetSocketAddress socksAddress, byte[] bytes) {
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(bytes), dstAddress);
        TernaryDatagramPacket msg = new TernaryDatagramPacket(data, socksAddress);
        logger.info("Send msg {}", msg);
        channel.writeAndFlush(msg);
    }
}
