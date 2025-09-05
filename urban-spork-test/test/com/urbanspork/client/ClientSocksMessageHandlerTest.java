package com.urbanspork.client;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClientSocksMessageHandlerTest {
    @Test
    void testUnsupportedMessage() {
        EmbeddedChannel channel = new EmbeddedChannel(new ClientSocksMessageHandler(null));
        channel.writeInbound(new DefaultSocks5PasswordAuthRequest("", ""));
        Assertions.assertFalse(channel.isActive());
    }

    @Test
    void testUnsupportedCommand() {
        EmbeddedChannel channel = new EmbeddedChannel(new ClientSocksMessageHandler(null));
        channel.writeInbound(new DefaultSocks5CommandRequest(Socks5CommandType.BIND, Socks5AddressType.DOMAIN, "", 0));
        Socks5CommandResponse response = channel.readOutbound();
        Assertions.assertNotEquals(Socks5CommandStatus.SUCCESS, response.status());
    }
}
