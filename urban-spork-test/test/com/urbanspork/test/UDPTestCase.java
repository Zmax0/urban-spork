package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.socks.Socks5DatagramPacketDecoder;
import com.urbanspork.common.protocol.socks.Socks5DatagramPacketEncoder;
import com.urbanspork.common.protocol.socks.Socks5Handshaking;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.udp.DelayTestServer;
import com.urbanspork.test.server.udp.SimpleTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UDPTestCase {

    private static final int[] PORTS = TestUtil.freePorts(4);
    private static final int[] DST_PORTS = Arrays.copyOfRange(PORTS, 2, 4);
    private final ExecutorService service = Executors.newFixedThreadPool(4);
    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private final ClientConfig config = TestUtil.testConfig(PORTS[0], PORTS[1]);
    private final Channel channel = initChannel();
    private Consumer<TernaryDatagramPacket> consumer;

    @BeforeAll
    void init() {
        LoggerFactory.getLogger(UDPTestCase.class);
        config.save();
    }

    @DisplayName("Launch udp test server")
    @Test
    @Order(1)
    void launchUDPTestServer() {
        Future<?> future1 = service.submit(() -> {
            try {
                SimpleTestServer.launch(DST_PORTS[0]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Assertions.assertFalse(future1.isDone());
        Future<?> future2 = service.submit(() -> {
            try {
                DelayTestServer.launch(DST_PORTS[1]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Assertions.assertFalse(future2.isDone());
    }

    @DisplayName("Launch client")
    @Test
    @Order(2)
    void launchClient() {
        Future<?> future = service.submit(() -> Client.main(null));
        Assertions.assertFalse(future.isDone());
    }

    @DisplayName("Launch server")
    @Test
    @Order(3)
    void launchServer() {
        Future<?> future = service.submit(() -> Server.main(null));
        Assertions.assertFalse(future.isDone());
    }

    @DisplayName("Handshake")
    @ParameterizedTest
    @ArgumentsSource(PortProvider.class)
    @Order(4)
    void handshake(int dstPort) throws InterruptedException, ExecutionException {
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", dstPort);
        Socks5Handshaking.Result result = Socks5Handshaking.noAuth(Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).await().get();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, result.response().status());
        result.sessionChannel().eventLoop().shutdownGracefully();
    }

    @DisplayName("Send string packet")
    @ParameterizedTest
    @ArgumentsSource(PortProvider.class)
    @Order(5)
    void sendStringPacket(int dstPort) throws InterruptedException {
        DefaultEventLoop executor = new DefaultEventLoop();
        Promise<Void> promise = new DefaultPromise<>(executor);
        InetSocketAddress socksAddress = new InetSocketAddress("localhost", config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", dstPort);
        consumer = msg -> {
            try {
                Assertions.assertEquals(dstAddress, msg.third());
            } catch (Exception e) {
                promise.setFailure(e);
            }
            promise.setSuccess(null);
        };
        String str = TestDice.randomString();
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(str.getBytes()), dstAddress);
        TernaryDatagramPacket msg = new TernaryDatagramPacket(data, socksAddress);
        channel.writeAndFlush(msg);
        Assertions.assertTrue(promise.await(DelayTestServer.MAX_DELAYED_SECOND, TimeUnit.SECONDS));
        executor.shutdownGracefully();
    }

    @AfterAll
    void shutdown() {
        group.shutdownGracefully();
        service.shutdown();
    }

    private Channel initChannel() {
        return new Bootstrap().group(group)
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
            .bind(0).syncUninterruptibly().channel();
    }

    static class PortProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Arrays.stream(DST_PORTS).mapToObj(Arguments::of);
        }
    }
}
