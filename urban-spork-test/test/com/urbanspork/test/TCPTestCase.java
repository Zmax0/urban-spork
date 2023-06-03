package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.socks.Socks5Handshaking;
import com.urbanspork.common.util.Dice;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.tcp.EchoTestServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TCPTestCase {

    private final DefaultEventLoop executor = new DefaultEventLoop();
    private final ExecutorService pool = Executors.newFixedThreadPool(1);
    private final int dstPort = TestUtil.freePorts(1)[0];

    @BeforeAll
    void launchEchoTestServer() throws ExecutionException, InterruptedException {
        launchEchoTestServer(pool, dstPort);
    }

    @ParameterizedTest
    @EnumSource(Protocols.class)
    void testProtocols(Protocols protocols) throws ExecutionException, InterruptedException {
        int[] ports = TestUtil.freePorts(2);
        ClientConfig config = TestUtil.testConfig(ports[0], ports[1]);
        ServerConfig serverConfig = config.getServers().get(0);
        serverConfig.setProtocol(protocols);
        ExecutorService service = Executors.newFixedThreadPool(2);
        launchServer(service, config);
        launchClient(service, config);
        handshakeAndSendBytes(config, dstPort);
        service.shutdownNow();
    }

    void launchEchoTestServer(ExecutorService service, int port) throws InterruptedException, ExecutionException {
        Promise<Channel> promise = new DefaultPromise<>(executor);
        service.submit(() -> EchoTestServer.launch(port, promise));
        InetSocketAddress address = (InetSocketAddress) promise.await().get().localAddress();
        Assertions.assertEquals(port, address.getPort());
    }

    void launchClient(ExecutorService service, ClientConfig config) throws InterruptedException, ExecutionException {
        Promise<ServerSocketChannel> promise = new DefaultPromise<>(executor);
        service.submit(() -> Client.launch(config, promise));
        Assertions.assertEquals(config.getPort(), promise.await().get().localAddress().getPort());
    }

    void launchServer(ExecutorService service, ClientConfig config) throws InterruptedException, ExecutionException {
        Promise<List<ServerSocketChannel>> promise = new DefaultPromise<>(executor);
        List<ServerConfig> configs = config.getServers();
        service.submit(() -> Server.launch(configs, promise));
        Assertions.assertEquals(configs.get(0).getPort(), promise.await().get().get(0).localAddress().getPort());
    }

    void handshakeAndSendBytes(ClientConfig config, int port) throws InterruptedException, ExecutionException {
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", port);
        Socks5Handshaking.Result result = Socks5Handshaking.noAuth(Socks5CommandType.CONNECT, proxyAddress, dstAddress).await().get();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, result.response().status());
        byte[] bytes = Dice.randomBytes(1024);
        Channel channel = result.sessionChannel();
        ChannelPromise promise = channel.newPromise();
        channel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                if (Arrays.equals(ByteBufUtil.getBytes(msg), bytes)) {
                    promise.setSuccess(null);
                } else {
                    promise.setFailure(AssertionFailureBuilder.assertionFailure().message("Msg is not equals").build());
                }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                promise.setFailure(cause);
            }
        });
        channel.writeAndFlush(Unpooled.wrappedBuffer(bytes));
        promise.await(10, TimeUnit.HOURS);
        Assertions.assertTrue(promise.isSuccess());
        channel.eventLoop().shutdownGracefully();
    }

    @AfterAll
    void shutdown() {
        pool.shutdownNow();
        executor.shutdownGracefully();
    }
}
