package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.WebSocketSetting;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
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
        ClientTcpRelayHandler.WebSocketCodec codec = new ClientTcpRelayHandler.WebSocketCodec(
                channel,
                config,
                new ClientTcpRelayHandler.InboundWriter(c -> {}, c -> c.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, new Socks5AddressType(-1)))),
                c -> {}
        );
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(codec);
        codec.userEventTriggered(pipeline.context(codec), WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT);
        DefaultSocks5CommandResponse response = channel.readOutbound();
        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.status().isSuccess());
    }
}
