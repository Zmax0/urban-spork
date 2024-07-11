package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.config.WebSocketSetting;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ClientTcpRelayHandlerTest {
    @Test
    void testBuildSslHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        Assertions.assertDoesNotThrow(() -> ClientTcpRelayHandler.buildSslHandler(channel, config));
        SslSetting ssl = new SslSetting();
        ssl.setVerifyHostname(false);
        config.setSsl(ssl);
        Assertions.assertDoesNotThrow(() -> ClientTcpRelayHandler.buildSslHandler(channel, config));
    }

    @Test
    void testBuildWebSocketHandler() {
        WebSocketSetting webSocket = new WebSocketSetting();
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setWs(webSocket);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ClientTcpRelayHandler.buildWebSocketHandler(config));
        webSocket.setPath("/ws");
        webSocket.setHeader(Map.of("Host", "localhost"));
        Assertions.assertDoesNotThrow(() -> ClientTcpRelayHandler.buildWebSocketHandler(config));
    }
}
