package com.urbanspork.server;

import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
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
        ServerInitializer initializer = new ServerInitializer(config, new Context());
        Assertions.assertThrows(IllegalArgumentException.class, () -> initializer.initChannel(channel));
        config.setSsl(SslUtil.getSslSetting());
        Assertions.assertDoesNotThrow(() -> initializer.initChannel(channel));
    }
}
