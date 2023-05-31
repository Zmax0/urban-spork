package com.urbanspork.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.socks.Socks5Handshaking;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.TestUtil;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientSocksHandshakeTestCase {

    private static final int[] PORTS = TestUtil.freePorts(2);
    private final DefaultEventLoop executor = new DefaultEventLoop();
    private Future<?> future;

    @BeforeAll
    static void init() {
        LoggerFactory.getLogger(ClientSocksHandshakeTestCase.class);
    }

    @Test
    void testUDPEnable() throws InterruptedException, ExecutionException {
        ClientConfig config = TestUtil.testConfig(PORTS[0], PORTS[1]);
        config.getServers().get(0).setProtocol(Protocols.vmess);
        launchClient(config);
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress1 = new InetSocketAddress("localhost", TestDice.randomPort());
        assertFailedHandshake(proxyAddress, dstAddress1);
    }

    @Test
    void testIllegalDstAddress() throws InterruptedException, ExecutionException {
        ClientConfig config = TestUtil.testConfig(PORTS[0], PORTS[1]);
        launchClient(config);
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress1 = new InetSocketAddress("localhost", 0);
        assertFailedHandshake(proxyAddress, dstAddress1);
    }

    @AfterEach
    void reset() {
        if (future != null) {
            future.cancel(true);
        }
    }

    @AfterAll()
    void shutdown() {
        executor.shutdownGracefully();
    }

    private void launchClient(ClientConfig config) throws InterruptedException {
        Promise<ServerSocketChannel> promise = new DefaultPromise<>(executor);
        future = Executors.newFixedThreadPool(1).submit(() -> Client.launch(config, promise));
        promise.await();
    }

    private static void assertFailedHandshake(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) throws InterruptedException, ExecutionException {
        Promise<Socks5Handshaking.Result> promise = Socks5Handshaking.noAuth(Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress);
        Socks5Handshaking.Result result = promise.await().get();
        Assertions.assertEquals(Socks5CommandStatus.FAILURE, result.response().status());
        result.sessionChannel().eventLoop().shutdownGracefully();
    }

}
