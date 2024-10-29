package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.protocol.Protocol;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ClientTcpRelayHandlerTest {
    @Test
    void testAddSslHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        Assertions.assertDoesNotThrow(() -> ClientTcpRelayHandler.addSslHandler(channel, config));
        config.setProtocol(Protocol.trojan);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ClientTcpRelayHandler.addSslHandler(channel, config));
        SslSetting ssl = new SslSetting();
        ssl.setVerifyHostname(false);
        config.setSsl(ssl);
        Assertions.assertDoesNotThrow(() -> ClientTcpRelayHandler.addSslHandler(channel, config));
    }

    @Test
    void testAddWebSocketHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        WebSocketSetting webSocket = new WebSocketSetting();
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setWs(webSocket);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ClientTcpRelayHandler.addWebSocketHandlers(channel, config));
        webSocket.setPath("/ws");
        webSocket.setHeader(Map.of("Host", "localhost"));
        Assertions.assertDoesNotThrow(() -> ClientTcpRelayHandler.addWebSocketHandlers(channel, config));
    }
}
