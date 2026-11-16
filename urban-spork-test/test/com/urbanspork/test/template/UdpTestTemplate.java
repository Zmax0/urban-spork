package com.urbanspork.test.template;

import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.protocol.HandshakeResult;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.client.Socks5Handshake;
import com.urbanspork.test.server.udp.DelayedEchoTestServer;
import com.urbanspork.test.server.udp.SimpleEchoTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
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
    protected final List<InetSocketAddress> dstAddress = new ArrayList<>();
    private final EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    protected Channel channel;
    private Consumer<DatagramPacketWrapper> consumer;
    protected SimpleEchoTestServer simpleEchoTestUdpServer;
    protected DelayedEchoTestServer delayedEchoTestUdpServer;

    @BeforeAll
    protected void initTemplate() throws IOException {
        launchUDPTestServer();
        initChannel();
    }

    private void launchUDPTestServer() throws IOException {
        simpleEchoTestUdpServer = SimpleEchoTestServer.launch(0);
        dstAddress.add(simpleEchoTestUdpServer.localAddress());
        delayedEchoTestUdpServer = DelayedEchoTestServer.launch(0);
        dstAddress.add(delayedEchoTestUdpServer.localAddress());
    }

    private void initChannel() {
        channel = new Bootstrap().group(group)
            .channel(NioDatagramChannel.class)
            .option(ChannelOption.RECVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(65535))
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        new DatagramPacketEncoder(),
                        new DatagramPacketDecoder(),
                        new SimpleChannelInboundHandler<DatagramPacketWrapper>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacketWrapper msg) {
                                logger.info("Receive msg {}", msg);
                                consumer.accept(msg);
                            }
                        }
                    );
                }
            })
            .bind("127.0.0.1", 0).syncUninterruptibly().channel();
    }

    protected void handshakeAndSendBytes(InetSocketAddress proxyAddress) throws InterruptedException, ExecutionException, TimeoutException {
        for (InetSocketAddress address : dstAddress) {
            handshakeAndSendBytes(proxyAddress, address);
        }
    }

    void handshakeAndSendBytes(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) throws InterruptedException, ExecutionException, TimeoutException {
        HandshakeResult<Socks5CommandResponse> result = Socks5Handshake
            .noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxyAddress, (InetSocketAddress) channel.localAddress()).get(10, TimeUnit.SECONDS);
        try {
            Socks5CommandResponse response = result.response();
            Assertions.assertEquals(Socks5CommandStatus.SUCCESS, response.status());
            // A large first datagram catches truncation before any adaptive buffer can grow.
            byte[] bytes = Dice.rollBytes(4096);
            InetSocketAddress expectedAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), dstAddress.getPort());
            CompletableFuture<Void> promise = new CompletableFuture<>();
            consumer = msg -> {
                try {
                    Assertions.assertEquals(expectedAddress, msg.server());
                    Assertions.assertArrayEquals(bytes, ByteBufUtil.getBytes(msg.packet().content()));
                    promise.complete(null);
                } catch (AssertionError e) {
                    promise.completeExceptionally(e);
                }
            };
            InetSocketAddress relayAddress = new InetSocketAddress(response.bndAddr(), response.bndPort());
            DatagramPacket data = new DatagramPacket(Unpooled.wrappedBuffer(bytes), dstAddress);
            channel.writeAndFlush(new DatagramPacketWrapper(data, relayAddress)).sync();
            promise.get(DelayedEchoTestServer.MAX_DELAYED_SECOND + 5L, TimeUnit.SECONDS);
        } finally {
            result.channel().close().sync();
        }
    }

    @AfterAll
    void cleanTemplate() {
        logger.info("close test servers");
        simpleEchoTestUdpServer.close();
        delayedEchoTestUdpServer.close();
        logger.info("close channel");
        this.channel.close().syncUninterruptibly();
        group.shutdownGracefully().syncUninterruptibly();
    }
}
