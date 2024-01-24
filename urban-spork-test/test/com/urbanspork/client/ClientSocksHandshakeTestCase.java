package com.urbanspork.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTestCase;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.socks.ClientHandshake;
import com.urbanspork.test.TestDice;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@DisplayName("Client - Socks Handshake")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientSocksHandshakeTestCase {
    private final EventLoopGroup group = new NioEventLoopGroup();

    @Test
    void testUdpEnable() throws InterruptedException, ExecutionException {
        ClientConfig config = ClientConfigTestCase.testConfig(0, 0);
        config.getServers().getFirst().setProtocol(Protocols.vmess);
        Map.Entry<ServerSocketChannel, DatagramChannel> client = ClientTestCase.asyncLaunchClient(config);
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress1 = new InetSocketAddress(InetAddress.getLoopbackAddress(), TestDice.rollPort());
        assertFailedHandshake(proxyAddress, dstAddress1);
        Client.close(client);
    }

    @Test
    void testIllegalDstAddress() throws InterruptedException, ExecutionException {
        ClientConfig config = ClientConfigTestCase.testConfig(0, 0);
        Map.Entry<ServerSocketChannel, DatagramChannel> client = ClientTestCase.asyncLaunchClient(config);
        InetSocketAddress proxyAddress = new InetSocketAddress(config.getPort());
        InetSocketAddress dstAddress1 = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        assertFailedHandshake(proxyAddress, dstAddress1);
        Client.close(client);
    }


    private void assertFailedHandshake(InetSocketAddress proxyAddress, InetSocketAddress dstAddress) {
        Assertions.assertThrows(ExecutionException.class, () -> ClientHandshake.noAuth(group, Socks5CommandType.UDP_ASSOCIATE, proxyAddress, dstAddress).get(10, TimeUnit.SECONDS));
    }
}
