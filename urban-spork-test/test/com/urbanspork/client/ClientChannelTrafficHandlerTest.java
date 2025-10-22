package com.urbanspork.client;

import com.urbanspork.test.TestDice;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

class ClientChannelTrafficHandlerTest {
    @Test
    void testGetter() {
        ClientChannelContext context = new ClientChannelContext(null, null, new HashMap<>());
        EmbeddedChannel channel = new EmbeddedChannel();
        int port = TestDice.rollPort();
        ClientChannelTrafficHandler handler = new ClientChannelTrafficHandler("localhost", port, context);
        channel.pipeline().addLast(handler);
        Assertions.assertEquals(0, handler.getDownloaded());
        Assertions.assertEquals(0, handler.getUploaded());
        Assertions.assertEquals(0, handler.getDlSpeed());
        Assertions.assertEquals(0, handler.getUlSpeed());
        Assertions.assertEquals("localhost:" + port, handler.getHost());
    }
}
