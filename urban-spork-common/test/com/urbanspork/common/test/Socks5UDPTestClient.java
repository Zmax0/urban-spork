package com.urbanspork.common.test;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.protocol.socks.Socks5Handshaking;
import com.urbanspork.common.protocol.socks.udp.Socks5DatagramPacketDecoder;
import com.urbanspork.common.protocol.socks.udp.Socks5DatagramPacketEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Promise;
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

    public static void main(String[] args) throws InterruptedException, IOException, ExecutionException {
        int proxyPort = 1089;
        String hostname = "localhost";
        try {
            ClientConfig config = ConfigHandler.read(ClientConfig.class);
            proxyPort = config.getPort();
            hostname = config.getCurrent().getHost();
        } catch (Exception ignore) {
        }
        InetSocketAddress dstAddress = new InetSocketAddress(hostname, SimpleUDPTestServer.PORT);
        Promise<Socks5Handshaking.Result> promise = Socks5Handshaking.udpAssociateNoAuth(new InetSocketAddress("localhost", proxyPort), dstAddress);
        Socks5Handshaking.Result result = promise.await().get();
        int bndPort = result.bndPort();
        logger.info("Associate port {}", bndPort);
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        Channel channel = new Bootstrap().group(bossGroup).channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.attr(AttributeKeys.SOCKS5_DST_ADDR).set(dstAddress);
                        ch.pipeline().addLast(
                                new Socks5DatagramPacketEncoder(),
                                new Socks5DatagramPacketDecoder(),
                                new SimpleChannelInboundHandler<Socks5DatagramPacketDecoder.Result>(false) {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, Socks5DatagramPacketDecoder.Result msg) {
                                        ByteBuf content = msg.data().content();
                                        logger.info("Receive msg {}", content.readCharSequence(content.readableBytes(), StandardCharsets.UTF_8));
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
            // Sends the received line to the server.
            channel.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(line.getBytes()), new InetSocketAddress("localhost", bndPort)));
        }
        result.sessionChannel().eventLoop().shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

}
