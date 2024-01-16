package com.urbanspork.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTestCase;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.socks.ClientHandshake;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.TestUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@DisplayName("Client - Socks Handshake")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientSocksHandshakeTestCase {
    private static final int[] PORTS = TestUtil.freePorts(2);
    private final EventLoopGroup group = new NioEventLoopGroup();
    private Future<?> future;

    @Test
    void testUdpEnable() throws InterruptedException {
        ClientConfig config = ClientConfigTestCase.testConfig(PORTS[0], PORTS[1]);
        config.getServers().getFirst().setProtocol(Protocols.vmess);
        future = ClientTestCase.asyncLaunchClient(config);
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress1 = new InetSocketAddress("localhost", TestDice.rollPort());
        assertFailedHandshake(proxyAddress, dstAddress1);
    }

    @Test
    void testIllegalDstAddress() throws InterruptedException {
        ClientConfig config = ClientConfigTestCase.testConfig(PORTS[0], PORTS[1]);
        future = ClientTestCase.asyncLaunchClient(config);
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

    private void assertFailedHandshake(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) {
        Assertions.assertThrows(ExecutionException.class, () -> ClientHandshake.noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).get(10, TimeUnit.SECONDS));
    }
}
