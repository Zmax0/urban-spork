package com.urbanspork.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.protocol.socks.Handshake;
import com.urbanspork.test.TestDice;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientSocksHandshakeTest {
    private final EventLoopGroup group = new NioEventLoopGroup();

    @Test
    void testUdpEnable() throws InterruptedException, ExecutionException {
        ClientConfig config = ClientConfigTest.testConfig(0, 0);
        config.getServers().getFirst().setProtocol(Protocol.vmess);
        Client.Instance client = ClientTest.asyncLaunchClient(config);
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress1 = new InetSocketAddress(InetAddress.getLoopbackAddress(), TestDice.rollPort());
        assertFailedHandshake(proxyAddress, dstAddress1);
        client.close();
    }

    @Test
    void testIllegalDstAddress() throws InterruptedException, ExecutionException {
        ClientConfig config = ClientConfigTest.testConfig(0, 0);
        Client.Instance client = ClientTest.asyncLaunchClient(config);
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress1 = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        assertFailedHandshake(proxyAddress, dstAddress1);
        client.close();
    }


    private void assertFailedHandshake(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) {
        Assertions.assertThrows(ExecutionException.class, () -> Handshake.noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).get(10, TimeUnit.SECONDS));
    }
}
