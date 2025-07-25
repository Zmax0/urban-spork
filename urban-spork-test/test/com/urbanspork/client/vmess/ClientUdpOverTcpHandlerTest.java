package com.urbanspork.client.vmess;

import com.urbanspork.client.ClientChannelContext;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.test.TestDice;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.InetSocketAddress;

class ClientUdpOverTcpHandlerTest {
    @Test
    void testConnectFailed() {
        ServerConfig config = ServerConfigTest.testConfig(TestDice.rollPort());
        config.setProtocol(Protocol.vmess);
        config.setPassword(TestDice.rollPassword(Protocol.vmess, CipherKind.chacha20_poly1305));
        EventLoopGroup executor = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        EmbeddedChannel channel = new EmbeddedChannel(new ClientUdpOverTcpHandler(new ClientChannelContext(config, null, null), executor));
        DatagramPacketWrapper packet = new DatagramPacketWrapper(new DatagramPacket(Unpooled.EMPTY_BUFFER, new InetSocketAddress(TestDice.rollPort())), new InetSocketAddress(0));
        Assertions.assertThrows(ConnectException.class, () -> channel.writeInbound(packet));
        channel.close();
    }
}
