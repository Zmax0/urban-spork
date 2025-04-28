package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.test.DnsUtil;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioIoHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ClientRelayHandlerTest {
    @Test
    void testAddSslHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        Assertions.assertDoesNotThrow(() -> ClientRelayHandler.addSslHandler(channel, config));
        config.setProtocol(Protocol.trojan);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ClientRelayHandler.addSslHandler(channel, config));
    }

    @Test
    void testAddWebSocketHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        WebSocketSetting webSocket = new WebSocketSetting();
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setWs(webSocket);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ClientRelayHandler.addWebSocketHandlers(channel, config));
        webSocket.setPath("/ws");
        webSocket.setHeader(Map.of("Host", "localhost"));
        Assertions.assertDoesNotThrow(() -> ClientRelayHandler.addWebSocketHandlers(channel, config));
    }

    @RepeatedTest(2)
    void testResolveServerHost() {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setHost("example.com");
        config.setDns(DnsUtil.getDnsSetting());
        String host = ClientRelayHandler.resolveServerHost(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()), config);
        Assertions.assertNotNull(host);
        Assertions.assertNotEquals(host, config.getHost());
    }

    @Test
    void testCanResolve() {
        Assertions.assertFalse(ClientRelayHandler.canResolve("192.168.89.9"));
        Assertions.assertFalse(ClientRelayHandler.canResolve("localhost"));
        Assertions.assertFalse(ClientRelayHandler.canResolve("abcd:ef01:2345:6789:abcd:ef01:2345:6789"));
        Assertions.assertTrue(ClientRelayHandler.canResolve("example.com"));
    }
}
