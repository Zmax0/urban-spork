package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
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
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TCPTestCase {

    private static final int[] PORTS = TestUtil.freePorts(3);
    private static final int DST_PORT = PORTS[2];
    private final ClientConfig config = TestUtil.testConfig(PORTS[0], PORTS[1]);
    private final ExecutorService service = Executors.newFixedThreadPool(3);
    private final DefaultEventLoop executor = new DefaultEventLoop();

    @BeforeAll
    void init() {
        LoggerFactory.getLogger(TCPTestCase.class);
        config.save();
    }

    @DisplayName("Launch echo test server")
    @Test
    @Order(1)
    void launchEchoTestServer() throws InterruptedException, ExecutionException {
        Promise<Channel> promise = new DefaultPromise<>(executor);
        service.submit(() -> EchoTestServer.launch(DST_PORT, promise));
        InetSocketAddress address = (InetSocketAddress) promise.await().get().localAddress();
        Assertions.assertEquals(DST_PORT, address.getPort());
    }

    @DisplayName("Launch client")
    @Test
    @Order(2)
    void launchClient() throws InterruptedException, ExecutionException {
        Promise<ServerSocketChannel> promise = new DefaultPromise<>(executor);
        service.submit(() -> Client.launch(config, promise));
        Assertions.assertEquals(config.getPort(), promise.await().get().localAddress().getPort());
    }

    @DisplayName("Launch server")
    @Test
    @Order(3)
    void launchServer() throws InterruptedException, ExecutionException {
        Promise<List<ServerSocketChannel>> promise = new DefaultPromise<>(executor);
        List<ServerConfig> configs = config.getServers();
        service.submit(() -> Server.launch(configs, promise));
        Assertions.assertEquals(configs.get(0).getPort(), promise.await().get().get(0).localAddress().getPort());
    }

    @DisplayName("Handshake and send bytes")
    @Test
    @Order(4)
    void handshakeAndSendBytes() throws InterruptedException, ExecutionException {
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", DST_PORT);
        Socks5Handshaking.Result result = Socks5Handshaking.noAuth(Socks5CommandType.CONNECT, proxyAddress, dstAddress).await().get();
        Assertions.assertEquals(Socks5CommandStatus.SUCCESS, result.response().status());
        byte[] bytes = Dice.randomBytes(1024);
        Channel channel = result.sessionChannel();
        ChannelPromise promise = channel.newPromise();
        channel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                Assertions.assertArrayEquals(ByteBufUtil.getBytes(msg), bytes);
                promise.setSuccess(null);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                promise.setFailure(cause);
            }
        });
        channel.writeAndFlush(Unpooled.wrappedBuffer(bytes));
        promise.await().get();
        Assertions.assertTrue(promise.isSuccess());
        channel.eventLoop().shutdownGracefully();
    }

    @AfterAll
    void shutdown() {
        service.shutdownNow();
    }
}
