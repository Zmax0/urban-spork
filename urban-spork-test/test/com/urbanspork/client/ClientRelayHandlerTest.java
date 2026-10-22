package com.urbanspork.client;

import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.test.SslUtil;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.server.tcp.DohTestServer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

class ClientRelayHandlerTest {
    @Test
    void testAddSslHandler() throws SSLException {
        EmbeddedChannel channel = new EmbeddedChannel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        ClientChannelContext context = new ClientChannelContext(config, null, null);
        Assertions.assertDoesNotThrow(() -> context.addSslHandler(channel));
        config.setProtocol(Protocol.trojan);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ClientChannelContext(config, null, null));
        channel.finishAndReleaseAll();
    }

    @Test
    void testCanResolve() {
        Assertions.assertFalse(ClientRelayHandler.canResolve("192.168.89.9"));
        Assertions.assertFalse(ClientRelayHandler.canResolve("localhost"));
        Assertions.assertFalse(ClientRelayHandler.canResolve("abcd:ef01:2345:6789:abcd:ef01:2345:6789"));
        Assertions.assertTrue(ClientRelayHandler.canResolve("example.com"));
    }

    @Test
    void testResolveServerHost() throws Exception {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try (DohTestServer dohServer = DohTestServer.launch(0)) {
            int dohPort = dohServer.localAddress().getPort();
            ServerConfig config = ServerConfigTest.testConfig(0);
            config.setHost(InetAddress.getLoopbackAddress().getHostName());
            DnsSetting dnsSetting = new DnsSetting(String.format("https://localhost:%d/dns-query?resolved=1.2.3.4", dohPort), SslUtil.getSslSetting(), 256);
            config.setDns(dnsSetting);
            ClientChannelContext context = new ClientChannelContext(config, null, null);
            Assertions.assertEquals(config.getHost(), ClientRelayHandler.resolveServerHost(group, context).get(10, TimeUnit.SECONDS));
            config.setHost("example.com");
            Promise<String> promise = ClientRelayHandler.resolveServerHost(group, context);
            Assertions.assertEquals("1.2.3.4", promise.get(20, TimeUnit.SECONDS));
            Assertions.assertEquals("1.2.3.4", dnsSetting.cache().get("example.com", Instant.now()).orElseThrow());
            Assertions.assertEquals("1.2.3.4", ClientRelayHandler.resolveServerHost(group, context).get(10, TimeUnit.SECONDS));
            String cachedHost = TestDice.rollHost();
            config.setHost(cachedHost);
            dnsSetting.cache().put(cachedHost, "1.2.3.4", 60, Instant.now());
            Assertions.assertEquals("1.2.3.4", ClientRelayHandler.resolveServerHost(group, context).get(10, TimeUnit.SECONDS));
            String unresolvableOnPeer = TestDice.rollHost();
            config.setHost(unresolvableOnPeer);
            config.setDns(new DnsSetting("https://localhost:1/dns-query", null));
            Assertions.assertEquals(unresolvableOnPeer, ClientRelayHandler.resolveServerHost(group, context).get(10, TimeUnit.SECONDS));
        } finally {
            group.shutdownGracefully();
        }
    }
}
