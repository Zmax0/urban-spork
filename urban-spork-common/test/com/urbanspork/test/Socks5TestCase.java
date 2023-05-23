package com.urbanspork.test;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.socks.Socks5DatagramPacketDecoder;
import com.urbanspork.common.protocol.socks.Socks5DatagramPacketEncoder;
import com.urbanspork.common.protocol.socks.Socks5Handshaking;
import com.urbanspork.test.server.DelayedUDPTestServer;
import com.urbanspork.test.server.SimpleUDPTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Tag("dependent")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Socks5TestCase {

    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private String dstHostname = "localhost";
    private int socksPort = 1089;
    private Channel channel;
    private Consumer<TernaryDatagramPacket> consumer;

    @BeforeAll
    void init() {
        try {
            ClientConfig config = ConfigHandler.read(ClientConfig.class);
            socksPort = config.getPort();
            dstHostname = config.getCurrent().getHost();
        } catch (Exception ignore) {}
        channel = new Bootstrap().group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                                new Socks5DatagramPacketEncoder(),
                                new Socks5DatagramPacketDecoder(),
                                new SimpleChannelInboundHandler<TernaryDatagramPacket>(false) {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, TernaryDatagramPacket msg) {
                                        consumer.accept(msg);
                                    }
                                }
                        );
                    }
                })
                .bind(TestDice.randomPort()).syncUninterruptibly().channel();
    }

    @ParameterizedTest
    @ValueSource(ints = {SimpleUDPTestServer.PORT, DelayedUDPTestServer.PORT})
    void testUdpAssociateNoAuth(int dstPort) throws InterruptedException, ExecutionException {
        InetSocketAddress proxyAddress = new InetSocketAddress(socksPort);
        InetSocketAddress dstAddress = new InetSocketAddress(this.dstHostname, dstPort);
        Socks5Handshaking.Result result = Socks5Handshaking.udpAssociateNoAuth(proxyAddress, dstAddress).await().get();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, result.response().status());
        result.sessionChannel().eventLoop().shutdownGracefully();
    }

    @ParameterizedTest
    @ValueSource(ints = {SimpleUDPTestServer.PORT, DelayedUDPTestServer.PORT})
    void testStringPacket(int dstPort) throws InterruptedException {
        DefaultEventLoop executor = new DefaultEventLoop();
        Promise<Void> promise = new DefaultPromise<>(executor);
        InetSocketAddress socksAddress = new InetSocketAddress("localhost", socksPort);
        InetSocketAddress dstAddress = new InetSocketAddress(dstHostname, dstPort);
        consumer = msg -> {
            try {
                Assertions.assertEquals(dstAddress, msg.third());
            } catch (Exception e) {
                promise.setFailure(e);
            }
            promise.setSuccess(null);
        };
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(TestDice.randomString().getBytes()), dstAddress);
        TernaryDatagramPacket msg = new TernaryDatagramPacket(data, socksAddress);
        channel.writeAndFlush(msg);
        Assertions.assertTrue(promise.await(20, TimeUnit.SECONDS));
        executor.shutdownGracefully();
    }

    @AfterAll
    void shutdown() {
        group.shutdownGracefully();
    }
}
