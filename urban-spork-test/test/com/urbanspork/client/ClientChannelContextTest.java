package com.urbanspork.client;

import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.test.SslUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

class ClientChannelContextTest {
    @Test
    void testSslContext() throws Exception {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setSsl(SslUtil.getSslSetting());
        ClientChannelContext context = new ClientChannelContext(config, null, null);
        Assertions.assertNotNull(context.sslContext());
        SslSetting withoutServerName = new SslSetting();
        withoutServerName.setCertificateFile(SslUtil.getSslSetting().getCertificateFile());
        config.setSsl(withoutServerName);
        ClientChannelContext fallback = new ClientChannelContext(config, null, null);
        EmbeddedChannel channel = new EmbeddedChannel();
        Assertions.assertDoesNotThrow(() -> fallback.addSslHandler(channel));
        Assertions.assertNotNull(channel.pipeline().get(SslHandler.class));
        channel.finishAndReleaseAll();
    }

    @Test
    void testQuicContextNotEnabled() throws Exception {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setSsl(SslUtil.getSslSetting());
        config.setTransport(new Transport[]{Transport.TCP});
        ClientChannelContext context = new ClientChannelContext(config, null, null);
        Assertions.assertNull(context.quicContext());
    }

    @Test
    void testQuicContextWithoutSsl() {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setTransport(new Transport[]{Transport.QUIC});
        config.setSsl(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ClientChannelContext(config, null, null));
    }

    @Test
    void testDohSslHandler() throws Exception {
        ServerConfig config = ServerConfigTest.testConfig(0);
        ClientChannelContext noDoh = new ClientChannelContext(config, null, null);
        EmbeddedChannel channel = new EmbeddedChannel();
        Assertions.assertThrows(IllegalStateException.class, () -> noDoh.newDohSslHandler(channel, new InetSocketAddress("127.0.0.1", 443)));
        DnsSetting dnsSetting = new DnsSetting("https://localhost:443/dns-query", SslUtil.getSslSetting());
        config.setDns(dnsSetting);
        ClientChannelContext withDoh = new ClientChannelContext(config, null, null);
        Assertions.assertNotNull(withDoh.newDohSslHandler(channel, new InetSocketAddress("localhost", 443)));
        channel.finishAndReleaseAll();
    }

    @Test
    void testDohSslHandlerWithoutSslSetting() throws Exception {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setDns(new DnsSetting("http://127.0.0.1/dns-query", null));
        ClientChannelContext context = new ClientChannelContext(config, null, null);
        Assertions.assertNotNull(context.dohSslContext());
    }

    @Test
    void testNullConfig() throws Exception {
        ClientChannelContext context = new ClientChannelContext(null, null, null);
        Assertions.assertNull(context.sslContext());
        Assertions.assertNull(context.quicContext());
        Assertions.assertNull(context.dohSslContext());
        EmbeddedChannel channel = new EmbeddedChannel();
        try {
            Assertions.assertDoesNotThrow(() -> context.addSslHandler(channel));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testTrojanWithoutSsl() {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setProtocol(Protocol.trojan);
        config.setSsl(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ClientChannelContext(config, null, null));
    }
}
