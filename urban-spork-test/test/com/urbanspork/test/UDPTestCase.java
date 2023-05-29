package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.common.protocol.shadowsocks.network.PacketEncoding;
import com.urbanspork.common.protocol.socks.Socks5DatagramPacketDecoder;
import com.urbanspork.common.protocol.socks.Socks5DatagramPacketEncoder;
import com.urbanspork.common.protocol.socks.Socks5Handshaking;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.udp.DelayedUDPTestServer;
import com.urbanspork.test.server.udp.SimpleUDPTestServer;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UDPTestCase {

    private static final int[] PORTS = TestUtil.freePorts(4);
    private static final int[] DST_PORTS = Arrays.copyOfRange(PORTS, 2, 4);

    private final ExecutorService service = Executors.newFixedThreadPool(4);
    private final EventLoopGroup group = new NioEventLoopGroup(1);
    private final String hostname = "localhost";
    private final ClientConfig config = initConfig();
    private Channel channel;
    private Consumer<TernaryDatagramPacket> consumer;

    @BeforeAll
    void init() {
        initDependent(DST_PORTS[0], DST_PORTS[1], config);
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
            .bind(0).syncUninterruptibly().channel();
    }

    private ClientConfig initConfig() {
        ClientConfig config = new ClientConfig();
        config.setPort(PORTS[1]);
        config.setIndex(0);
        config.setServers(List.of(initServerConfig()));
        return config;
    }

    private ServerConfig initServerConfig() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setHost(hostname);
        serverConfig.setPort(PORTS[0]);
        serverConfig.setProtocol(Protocols.shadowsocks);
        serverConfig.setCipher(SupportedCipher.chacha20_poly1305);
        serverConfig.setPassword(UUID.randomUUID().toString());
        serverConfig.setNetworks(new Network[]{Network.UDP});
        serverConfig.setPacketEncoding(PacketEncoding.Packet);
        return serverConfig;
    }

    @ParameterizedTest
    @ArgumentsSource(PortProvider.class)
    void testNoAuthUdpAssociate(int dstPort) throws InterruptedException, ExecutionException {
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress(this.hostname, dstPort);
        Socks5Handshaking.Result result = Socks5Handshaking.noAuth(Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).await().get();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, result.response().status());
        result.sessionChannel().eventLoop().shutdownGracefully();
    }

    @ParameterizedTest
    @ArgumentsSource(PortProvider.class)
    void testStringPacket(int dstPort) throws InterruptedException {
        DefaultEventLoop executor = new DefaultEventLoop();
        Promise<Void> promise = new DefaultPromise<>(executor);
        InetSocketAddress socksAddress = new InetSocketAddress("localhost", config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress(hostname, dstPort);
        consumer = msg -> {
            try {
                Assertions.assertEquals(dstAddress, msg.third());
            } catch (Exception e) {
                promise.setFailure(e);
            }
            promise.setSuccess(null);
        };
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer("Socks5UDPCOMMTestCase".getBytes()), dstAddress);
        TernaryDatagramPacket msg = new TernaryDatagramPacket(data, socksAddress);
        channel.writeAndFlush(msg);
        Assertions.assertTrue(promise.await(6, TimeUnit.SECONDS));
        executor.shutdownGracefully();
    }

    @AfterAll
    void shutdown() {
        group.shutdownGracefully();
        service.shutdown();
    }

    private void initDependent(int port1, int port2, ClientConfig config) {
        service.submit(() -> {
            try {
                SimpleUDPTestServer.launch(port1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        service.submit(() -> {
            try {
                DelayedUDPTestServer.launch(port2);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        service.submit(() -> Client.launch(config));
        service.submit(() -> Server.launch(config.getServers()));
    }

    static class PortProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Arrays.stream(DST_PORTS).mapToObj(Arguments::of);
        }
    }
}
