package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.WebSocketSetting;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ClientSocksConnectHandlerTest {
    @Test
    void testWebSocketHandshakeTimeout() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        WebSocketSetting webSocket = new WebSocketSetting();
        webSocket.setPath("/ws");
        webSocket.setHeader(Map.of("Host", "localhost"));
        config.setWs(webSocket);
        DefaultSocks5CommandRequest request = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, "localhost", 0);
        ClientSocksConnectHandler.WebSocketCodec codec = new ClientSocksConnectHandler.WebSocketCodec(channel, config, request);
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(codec);
        codec.userEventTriggered(pipeline.context(codec), WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT);
        DefaultSocks5CommandResponse response = channel.readOutbound();
        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.status().isSuccess());
    }
}
