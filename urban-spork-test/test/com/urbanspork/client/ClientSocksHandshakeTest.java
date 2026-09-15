package com.urbanspork.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.protocol.HandshakeResult;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.client.Socks5Handshake;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientSocksHandshakeTest {
    private final EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    @Test
    void testUdpEnable() throws InterruptedException, ExecutionException, TimeoutException {
        ClientConfig config = ClientConfigTest.testConfig(0, 0);
        config.getServers().getFirst().setProtocol(Protocol.vmess);
        Client.Instance client = ClientTest.asyncLaunchClient(config);
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress1 = new InetSocketAddress(InetAddress.getLoopbackAddress(), TestDice.rollPort());
        assertFailedHandshake(proxyAddress, dstAddress1, Socks5CommandStatus.FORBIDDEN);
        client.close();
    }

    @Test
    void testIllegalDstAddress() throws InterruptedException, ExecutionException, TimeoutException {
        ClientConfig config = ClientConfigTest.testConfig(0, 0);
        Client.Instance client = ClientTest.asyncLaunchClient(config);
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress1 = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        assertFailedHandshake(proxyAddress, dstAddress1, Socks5CommandStatus.FAILURE);
        client.close();
    }


    private void assertFailedHandshake(InetSocketAddress proxyAddress, InetSocketAddress dstAddress, Socks5CommandStatus expectedStatus)
        throws InterruptedException, ExecutionException, TimeoutException {
        HandshakeResult<Socks5CommandResponse> result = Socks5Handshake
            .noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).get(10, TimeUnit.SECONDS);
        try {
            Assertions.assertEquals(expectedStatus, result.response().status());
        } finally {
            result.channel().close().sync();
        }
    }
}
