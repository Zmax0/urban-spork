package com.urbanspork.server;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.transport.tcp.RelayingPayload;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

class ServerRelayHandlerTest {
    @Test
    void testReadUnexpectedMsg() {
        ServerConfig config = ServerConfigTest.testConfig(0);
        EmbeddedChannel channel = new EmbeddedChannel(new ServerRelayHandler(config));
        ByteBuf msg = Unpooled.wrappedBuffer(Dice.rollBytes(10));
        Assertions.assertThrows(NullPointerException.class, () -> channel.writeInbound(msg));
    }

    @Test
    void testConnectFailed() {
        ServerConfig config = ServerConfigTest.testConfig(0);
        EmbeddedChannel channel = new EmbeddedChannel(new ServerRelayHandler(config));
        channel.writeInbound(new RelayingPayload<>(new InetSocketAddress(0), Unpooled.wrappedBuffer(Dice.rollBytes(10))));
        Assertions.assertFalse(channel.isActive());
    }
}
