package com.urbanspork.server;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

@DisplayName("Server - Remote Connect Handler")
class RemoteConnectHandlerTestCase {
    @Test
    void testConnectFailed() {
        ServerConfig config = ServerConfigTestCase.testConfig(0);
        EmbeddedChannel channel = new EmbeddedChannel(new RemoteConnectHandler(config));
        channel.writeInbound(new InetSocketAddress(0));
        Assertions.assertFalse(channel.isActive());
    }
}
