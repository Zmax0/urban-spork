package com.urbanspork.test.template;

import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.protocol.HandshakeResult;
import com.urbanspork.common.protocol.socks.Handshake;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.server.udp.DelayedEchoTestServer;
import com.urbanspork.test.server.udp.SimpleEchoTestServer;
import io.netty.bootstrap.Bootstrap;
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
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class UdpTestTemplate extends TestTemplate {
    private static final Logger logger = LoggerFactory.getLogger(UdpTestTemplate.class);
    protected final List<InetSocketAddress> dstAddress = new ArrayList<>();
    private final EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    protected Channel channel;
    private Consumer<DatagramPacketWrapper> consumer;
    protected DatagramSocket simpleEchoTestUdpServer;
    private ServerSocket simpleEchoTestTcpServer;
    protected DatagramSocket delayedEchoTestUdpServer;
    private ServerSocket delayedEchoTestTcpServer;

    @BeforeAll
    protected void beforeAll() {
        launchUDPTestServer();
        initChannel();
    }

    private void launchUDPTestServer() {
        CompletableFuture<DatagramSocket> f1 = new CompletableFuture<>();
        POOL.submit(() -> {
            try {
                SimpleEchoTestServer.launch(0, f1);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        try {
            simpleEchoTestUdpServer = f1.get();
            int localPort = simpleEchoTestUdpServer.getLocalPort();
            simpleEchoTestTcpServer = new ServerSocket(localPort); // bind tcp at same time
            dstAddress.add(new InetSocketAddress(InetAddress.getLoopbackAddress(), localPort));
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } catch (Exception _) {
            Assertions.fail("launch test server failed");
        }
        CompletableFuture<DatagramSocket> f2 = new CompletableFuture<>();
        POOL.submit(() -> {
            try {
                DelayedEchoTestServer.launch(0, f2);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            delayedEchoTestUdpServer = f2.get();
            int localPort = delayedEchoTestUdpServer.getLocalPort();
            delayedEchoTestTcpServer = new ServerSocket(localPort);  // bind tcp at same time
            dstAddress.add(new InetSocketAddress(InetAddress.getLoopbackAddress(), localPort));
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } catch (Exception _) {
            Assertions.fail("launch test server failed");
        }
    }

    private void initChannel() {
        channel = new Bootstrap().group(group)
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
                                logger.info("Receive msg {}", msg);
                                consumer.accept(msg);
                            }
                        }
                    );
                }
            })
            .bind(0).syncUninterruptibly().channel();
    }

    protected void handshakeAndSendBytes(InetSocketAddress proxyAddress) throws InterruptedException, ExecutionException, TimeoutException {
        for (InetSocketAddress address : dstAddress) {
            handshakeAndSendBytes(proxyAddress, address);
        }
    }

    void handshakeAndSendBytes(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) throws InterruptedException, ExecutionException, TimeoutException {
        HandshakeResult<Socks5CommandResponse> result = Handshake.noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).get();
        result.channel().close().sync();
        Socks5CommandResponse response = result.response();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, response.status());
        CompletableFuture<Void> promise = new CompletableFuture<>();
        consumer = msg -> {
            if (dstAddress.isUnresolved() || dstAddress.equals(msg.server())) {
                promise.complete(null);
            } else {
                promise.completeExceptionally(AssertionFailureBuilder.assertionFailure().message("Not equals").build());
            }
        };
        String str = TestDice.rollString();
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(str.getBytes()), dstAddress);
        DatagramPacketWrapper msg = new DatagramPacketWrapper(data, proxyAddress);
        logger.info("Send msg {}", msg);
        channel.writeAndFlush(msg);
        promise.get(DelayedEchoTestServer.MAX_DELAYED_SECOND + 3L, TimeUnit.SECONDS);
        Assertions.assertTrue(promise.isDone());
    }

    @AfterAll
    void shutdown() throws IOException {
        simpleEchoTestUdpServer.close();
        simpleEchoTestTcpServer.close();
        delayedEchoTestUdpServer.close();
        delayedEchoTestTcpServer.close();
        channel.close();
        group.shutdownGracefully();
    }
}
