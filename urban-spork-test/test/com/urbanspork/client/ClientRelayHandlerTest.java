package com.urbanspork.client;

import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.test.TestDice;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioIoHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ClientRelayHandlerTest {
    @Test
    void testAddSslHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        ClientChannelContext context = new ClientChannelContext(config, null, null);
        Assertions.assertDoesNotThrow(() -> ClientRelayHandler.addSslHandler(channel, context));
        config.setProtocol(Protocol.trojan);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ClientRelayHandler.addSslHandler(channel, context));
    }

    @Test
    void testAddWebSocketHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        WebSocketSetting webSocket = new WebSocketSetting();
        ServerConfig config = ServerConfigTest.testConfig(0);
        ClientChannelContext context = new ClientChannelContext(config, null, null);
        config.setWs(webSocket);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ClientRelayHandler.addWebSocketHandlers(channel, context));
        webSocket.setPath("/ws");
        webSocket.setHeader(Map.of("Host", "localhost"));
        Assertions.assertDoesNotThrow(() -> ClientRelayHandler.addWebSocketHandlers(channel, context));
    }

    @Test
    void testTryResolveServerHost() throws ExecutionException, InterruptedException, TimeoutException {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setHost(TestDice.rollHost());
        int port = TestDice.rollPort();
        String nameServer = "https://255.255.255.255:" + port + "/dns-query";
        DnsSetting dnsSetting = new DnsSetting(nameServer, null);
        config.setDns(dnsSetting);
        String host = "1.2.3.4";
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            String resolved = ClientRelayHandler.resolveServerHost(group, config).get(10, TimeUnit.SECONDS);
            Assertions.assertEquals(config.getHost(), resolved);
            dnsSetting.cache().put(config.getHost(), host, 60, Instant.now());
            resolved = ClientRelayHandler.resolveServerHost(group, config).get(10, TimeUnit.SECONDS);
            Assertions.assertEquals(host, resolved);
        } finally {
            group.shutdownGracefully();
        }
    }

    @Test
    void testCanResolve() {
        Assertions.assertFalse(ClientRelayHandler.canResolve("192.168.89.9"));
        Assertions.assertFalse(ClientRelayHandler.canResolve("localhost"));
        Assertions.assertFalse(ClientRelayHandler.canResolve("abcd:ef01:2345:6789:abcd:ef01:2345:6789"));
        Assertions.assertTrue(ClientRelayHandler.canResolve("example.com"));
    }


}
