package com.urbanspork.server;

import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.test.SslUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;

class ServerInitializerTest {
    @Test
    void outboundSslChanelFailed() throws SSLException {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setProtocol(Protocol.trojan);
        EmbeddedChannel channel = new EmbeddedChannel();
        ServerInitializer initializer = new ServerInitializer(new ServerChannelContext(config, new Context()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> initializer.initChannel(channel));
        SslSetting sslSetting = SslUtil.getSslSetting();
        sslSetting.setServerName(null);
        config.setSsl(sslSetting);
        ServerInitializer tlsInitializer = new ServerInitializer(new ServerChannelContext(config, new Context()));
        Assertions.assertDoesNotThrow(() -> tlsInitializer.initChannel(channel));
    }

    @Test
    void outboundWsChannelFailed() throws SSLException {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setWs(new WebSocketSetting());
        EmbeddedChannel channel = new EmbeddedChannel();
        ServerChannelContext context = new ServerChannelContext(config, new Context());
        ServerInitializer initializer = new ServerInitializer(context);
        Assertions.assertThrows(NullPointerException.class, () -> initializer.initChannel(channel));
    }
}
