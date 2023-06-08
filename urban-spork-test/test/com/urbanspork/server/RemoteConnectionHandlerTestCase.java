package com.urbanspork.server;

import com.urbanspork.test.TestUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

@DisplayName("Server - Remote Connection Handler")
class RemoteConnectionHandlerTestCase {
    @Test
    void testConnectFailed() {
        EmbeddedChannel channel = new EmbeddedChannel(new RemoteConnectionHandler());
        channel.writeInbound(new InetSocketAddress(TestUtil.freePort()));
        Assertions.assertFalse(channel.isActive());
    }
}
