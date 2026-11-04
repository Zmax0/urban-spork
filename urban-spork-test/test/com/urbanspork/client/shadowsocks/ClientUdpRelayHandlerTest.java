package com.urbanspork.client.shadowsocks;

import com.urbanspork.client.ClientChannelContext;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.test.SslUtil;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

class ClientUdpRelayHandlerTest {
    @Test
    void testBindingChannelFailed() throws Exception {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setHost(InetAddress.getLoopbackAddress().getHostName());
        config.setSsl(SslUtil.getSslSetting());
        ClientUdpRelayHandler handler = new ClientUdpRelayHandler(new ClientChannelContext(config, null, null));
        EmbeddedChannel inbound = new EmbeddedChannel();
        Promise<Channel> promise = inbound.eventLoop().newPromise();
        handler.newBindingChannel(inbound, new InetSocketAddress("127.0.0.1", 1), promise);
        Assertions.assertTrue(promise.await(5, TimeUnit.SECONDS));
        Assertions.assertFalse(promise.isSuccess());
        Assertions.assertInstanceOf(IllegalStateException.class, promise.cause());
        handler.handlerRemoved(null);
        inbound.finishAndReleaseAll();
    }

}
