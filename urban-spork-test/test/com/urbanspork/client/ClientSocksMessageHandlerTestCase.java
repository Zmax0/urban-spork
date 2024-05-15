package com.urbanspork.client;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Client - Socks Message Handler")
class ClientSocksMessageHandlerTestCase {
    @Test
    void testUnsupportedMessage() {
        EmbeddedChannel channel = new EmbeddedChannel(ClientSocksMessageHandler.INSTANCE);
        channel.writeInbound(new DefaultSocks5PasswordAuthRequest("", ""));
        Assertions.assertFalse(channel.isActive());
    }

    @Test
    void testUnsupportedCommand() {
        EmbeddedChannel channel = new EmbeddedChannel(ClientSocksMessageHandler.INSTANCE);
        channel.writeInbound(new DefaultSocks5CommandRequest(Socks5CommandType.BIND, Socks5AddressType.DOMAIN, "", 0));
        Socks5CommandResponse response = channel.readOutbound();
        Assertions.assertNotEquals(Socks5CommandStatus.SUCCESS, response.status());
    }
}
