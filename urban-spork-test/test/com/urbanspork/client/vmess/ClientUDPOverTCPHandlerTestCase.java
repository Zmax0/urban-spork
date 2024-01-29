package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import com.urbanspork.test.TestDice;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.InetSocketAddress;

@DisplayName("VMess - Client UDP Over TCP Handler")
class ClientUDPOverTCPHandlerTestCase {
    @Test
    void testConnectFailed() {
        ServerConfig config = ServerConfigTestCase.testConfig(TestDice.rollPort());
        config.setProtocol(Protocols.vmess);
        config.setPassword(TestDice.rollPassword(Protocols.vmess, CipherKind.chacha20_poly1305));
        EventLoopGroup executor = new NioEventLoopGroup();
        EmbeddedChannel channel = new EmbeddedChannel(new ClientUdpOverTCPHandler(config, executor));
        TernaryDatagramPacket packet = new TernaryDatagramPacket(new DatagramPacket(Unpooled.EMPTY_BUFFER, new InetSocketAddress(TestDice.rollPort())), new InetSocketAddress(0));
        Assertions.assertThrows(ConnectException.class, () -> channel.writeInbound(packet));
        channel.close();
    }
}
