package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.config.WebSocketSetting;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ClientSocksInitializerTest {
    @Test
    void testBuildSslHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        Assertions.assertDoesNotThrow(() -> ClientSocksInitializer.buildSslHandler(channel, config));
        SslSetting ssl = new SslSetting();
        ssl.setVerifyHostname(false);
        config.setSsl(ssl);
        Assertions.assertDoesNotThrow(() -> ClientSocksInitializer.buildSslHandler(channel, config));
    }

    @Test
    void testBuildWebSocketHandler() {
        ServerConfig config = ServerConfigTest.testConfig(0);
        Assertions.assertThrows(NullPointerException.class, () -> ClientSocksInitializer.buildWebSocketHandler(config));
        WebSocketSetting webSocket = new WebSocketSetting();
        webSocket.setPath("/ws");
        webSocket.setHeader(Map.of("Host", "localhost"));
        config.setWs(webSocket);
        Assertions.assertDoesNotThrow(() -> ClientSocksInitializer.buildWebSocketHandler(config));
    }
}
