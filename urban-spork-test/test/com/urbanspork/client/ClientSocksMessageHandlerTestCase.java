package com.urbanspork.client;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
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
}
