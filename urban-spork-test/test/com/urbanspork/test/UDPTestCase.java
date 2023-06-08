package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTestCase;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.common.protocol.socks.DatagramPacketDecoder;
import com.urbanspork.common.protocol.socks.DatagramPacketEncoder;
import com.urbanspork.common.protocol.socks.Handshake;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.udp.DelayedEchoTestServer;
import com.urbanspork.test.server.udp.SimpleEchoTestServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.ServerSocketChannel;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@DisplayName("UDP")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UDPTestCase {

    private static final Logger logger = LoggerFactory.getLogger(UDPTestCase.class);
    private static final int[] PORTS = TestUtil.freePorts(4);
    private static final int[] DST_PORTS = Arrays.copyOfRange(PORTS, 2, 4);
    private final ClientConfig config = ClientConfigTestCase.testConfig(PORTS[0], PORTS[1]);
    private final ExecutorService service = Executors.newFixedThreadPool(4);
    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private final DefaultEventLoop executor = new DefaultEventLoop();
    private final Channel channel = initChannel();
    private Consumer<TernaryDatagramPacket> consumer;

    @DisplayName("launch udp test server")
    @Test
    @Order(1)
    void launchUDPTestServer() {
        Future<?> future1 = service.submit(() -> {
            try {
                SimpleEchoTestServer.launch(DST_PORTS[0]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Assertions.assertFalse(future1.isCancelled());
        Future<?> future2 = service.submit(() -> {
            try {
                DelayedEchoTestServer.launch(DST_PORTS[1]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Assertions.assertFalse(future2.isCancelled());
    }

    @DisplayName("launch client")
    @Test
    @Order(2)
    void launchClient() throws InterruptedException, ExecutionException {
        Promise<ServerSocketChannel> promise = new DefaultPromise<>(executor);
        service.submit(() -> Client.launch(config, promise));
        Assertions.assertEquals(config.getPort(), promise.await().get().localAddress().getPort());
    }

    @DisplayName("launch server")
    @Test
    @Order(3)
    void launchServer() throws InterruptedException, ExecutionException {
        Promise<List<ServerSocketChannel>> promise = new DefaultPromise<>(executor);
        List<ServerConfig> configs = config.getServers();
        for (ServerConfig config : configs) {
            config.setNetworks(new Network[]{Network.TCP, Network.UDP});
        }
        service.submit(() -> Server.launch(configs, promise));
        Assertions.assertEquals(configs.get(0).getPort(), promise.await().get().get(0).localAddress().getPort());
    }

    @DisplayName("handshake")
    @ParameterizedTest
    @ArgumentsSource(PortProvider.class)
    @Order(4)
    void handshake(int dstPort) throws InterruptedException, ExecutionException {
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", dstPort);
        Handshake.Result result = Handshake.noAuth(Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).await().get();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, result.response().status());
        result.sessionChannel().eventLoop().shutdownGracefully();
    }

    @DisplayName("send string packet")
    @ParameterizedTest
    @ArgumentsSource(PortProvider.class)
    @Order(5)
    void sendStringPacket(int dstPort) throws InterruptedException {
        DefaultEventLoop executor = new DefaultEventLoop();
        Promise<Void> promise = new DefaultPromise<>(executor);
        InetSocketAddress socksAddress = new InetSocketAddress("localhost", config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", dstPort);
        consumer = msg -> {
            if (dstAddress.equals(msg.third())) {
                promise.setSuccess(null);
            } else {
                promise.setFailure(AssertionFailureBuilder.assertionFailure().message("Not equals").build());
            }
        };
        String str = TestDice.randomString();
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(str.getBytes()), dstAddress);
        TernaryDatagramPacket msg = new TernaryDatagramPacket(data, socksAddress);
        logger.info("Send msg {}", msg);
        channel.writeAndFlush(msg);
        Assertions.assertTrue(promise.await(DelayedEchoTestServer.MAX_DELAYED_SECOND, TimeUnit.SECONDS));
        executor.shutdownGracefully();
    }

    @AfterAll
    void shutdown() {
        group.shutdownGracefully();
        service.shutdownNow();
    }

    private Channel initChannel() {
        return new Bootstrap().group(group)
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

    private static class PortProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Arrays.stream(DST_PORTS).mapToObj(Arguments::of);
        }
    }
}
