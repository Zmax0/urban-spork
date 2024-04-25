package com.urbanspork.server;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import com.urbanspork.common.transport.tcp.RelayingPayload;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

@DisplayName("Server - Server Relay Handler")
class ServerRelayHandlerTestCase {
    @Test
    void testConnectFailed() {
        ServerConfig config = ServerConfigTestCase.testConfig(0);
        EmbeddedChannel channel = new EmbeddedChannel(new ServerRelayHandler(config));
        channel.writeInbound(new RelayingPayload<>(new InetSocketAddress(0), Unpooled.wrappedBuffer(Dice.rollBytes(10))));
        Assertions.assertFalse(channel.isActive());
    }
}
