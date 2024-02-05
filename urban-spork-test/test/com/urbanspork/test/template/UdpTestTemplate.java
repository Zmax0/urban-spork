package com.urbanspork.test.template;

import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.socks.ClientHandshake;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.server.udp.DelayedEchoTestServer;
import com.urbanspork.test.server.udp.SimpleEchoTestServer;
import io.netty.bootstrap.Bootstrap;
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
    private final List<InetSocketAddress> dstAddress = new ArrayList<>();
    private final EventLoopGroup group = new NioEventLoopGroup();
    private Channel channel;
    private Consumer<TernaryDatagramPacket> consumer;
    private DatagramSocket simpleEchoTestServer;
    private DatagramSocket delayedEchoTestServer;

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
            simpleEchoTestServer = f1.get();
            dstAddress.add(new InetSocketAddress(InetAddress.getLoopbackAddress(), simpleEchoTestServer.getLocalPort()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
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
            delayedEchoTestServer = f2.get();
            dstAddress.add(new InetSocketAddress(InetAddress.getLoopbackAddress(), delayedEchoTestServer.getLocalPort()));
        } catch (ExecutionException e) {
            Assertions.fail("launch test server failed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
                        new SimpleChannelInboundHandler<TernaryDatagramPacket>(false) {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, TernaryDatagramPacket msg) {
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
        ClientHandshake.Result result = ClientHandshake.noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).await().get();
        result.sessionChannel().close();
        Socks5CommandResponse response = result.response();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, response.status());
        CompletableFuture<Void> promise = new CompletableFuture<>();
        consumer = msg -> {
            if (dstAddress.equals(msg.third())) {
                promise.complete(null);
            } else {
                promise.completeExceptionally(AssertionFailureBuilder.assertionFailure().message("Not equals").build());
            }
        };
        String str = TestDice.rollString();
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(str.getBytes()), dstAddress);
        TernaryDatagramPacket msg = new TernaryDatagramPacket(data, proxyAddress);
        logger.info("Send msg {}", msg);
        channel.writeAndFlush(msg);
        promise.get(DelayedEchoTestServer.MAX_DELAYED_SECOND + 3, TimeUnit.SECONDS);
        Assertions.assertTrue(promise.isDone());
    }

    @AfterAll
    void shutdown() {
        simpleEchoTestServer.close();
        delayedEchoTestServer.close();
        channel.close();
        group.shutdownGracefully();
    }
}
