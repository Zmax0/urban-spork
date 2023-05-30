package com.urbanspork.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.socks.Socks5Handshaking;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.TestUtil;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientSocksHandshakeTestCase {

    private static final int[] PORTS = TestUtil.freePorts(2);
    private Future<?> future;

    @BeforeAll
    static void init() {
        LoggerFactory.getLogger(ClientSocksHandshakeTestCase.class);
    }

    @Test
    void testUDPEnable() throws InterruptedException, ExecutionException {
        ClientConfig config = TestUtil.testConfig(PORTS[0], PORTS[1]);
        config.getServers().get(0).setProtocol(Protocols.vmess);
        future = Executors.newFixedThreadPool(1).submit(() -> Client.launch(config));
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress1 = new InetSocketAddress("localhost", TestDice.randomPort());
        assertFailedHandshake(proxyAddress, dstAddress1);
    }

    @Test
    void testIllegalDstAddress() throws InterruptedException, ExecutionException {
        ClientConfig config = TestUtil.testConfig(PORTS[0], PORTS[1]);
        future = Executors.newFixedThreadPool(1).submit(() -> Client.launch(config));
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
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

    private static void assertFailedHandshake(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) throws InterruptedException, ExecutionException {
        Promise<Socks5Handshaking.Result> promise = Socks5Handshaking.noAuth(Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress);
        Socks5Handshaking.Result result = promise.await().get();
        Assertions.assertEquals(Socks5CommandStatus.FAILURE, result.response().status());
        result.sessionChannel().eventLoop().shutdownGracefully();
    }

}
