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

class ServerInitializerTest {
    @Test
    void initSslChanelFailed() {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setProtocol(Protocol.trojan);
        EmbeddedChannel channel = new EmbeddedChannel();
        ServerInitializationContext context = new ServerInitializationContext(config, new Context());
        ServerInitializer initializer = new ServerInitializer(context);
        Assertions.assertThrows(IllegalArgumentException.class, () -> initializer.initChannel(channel));
        SslSetting sslSetting = SslUtil.getSslSetting();
        sslSetting.setServerName(null);
        config.setSsl(sslSetting);
        Assertions.assertDoesNotThrow(() -> initializer.initChannel(channel));
    }

    @Test
    void initWsChannelFailed() {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setWs(new WebSocketSetting());
        EmbeddedChannel channel = new EmbeddedChannel();
        ServerInitializationContext context = new ServerInitializationContext(config, new Context());
        ServerInitializer initializer = new ServerInitializer(context);
        Assertions.assertThrows(IllegalArgumentException.class, () -> initializer.initChannel(channel));
    }
}
