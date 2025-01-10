package com.urbanspork.client;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.test.SslUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
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

    @Test
    void testConnectQuicServerFailed() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Channel inbound = new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(new ChannelInboundHandlerAdapter()).bind(0).sync().channel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setSsl(SslUtil.getSslSetting());
        config.setTransport(new Transport[]{Transport.QUIC});
        inbound.attr(AttributeKeys.SERVER_CONFIG).set(config);
        Promise<Boolean> promise = group.next().newPromise();
        ClientTcpRelayHandler.InboundReady inboundReady = new ClientTcpRelayHandler.InboundReady(
            c -> promise.setSuccess(true), c -> promise.setSuccess(false)
        );
        ClientTcpRelayHandler testHandler = new ClientTcpRelayHandler() {
            @Override
            public InboundReady inboundReady() {
                return inboundReady;
            }
        };
        testHandler.connect(inbound, InetSocketAddress.createUnresolved("localhost", 0));
        Boolean res = promise.get();
        Assertions.assertFalse(res);
    }
}
