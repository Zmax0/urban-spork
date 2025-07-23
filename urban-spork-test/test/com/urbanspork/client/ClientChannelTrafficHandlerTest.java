package com.urbanspork.client;

import com.urbanspork.test.TestDice;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Optional;

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

    @Test
    void testEqualsAndHashCode() {
        ClientChannelContext context = new ClientChannelContext(null, null, new HashMap<>());
        ClientChannelTrafficHandler handler1 = new ClientChannelTrafficHandler("", 0, context);
        ClientChannelTrafficHandler handler2 = new ClientChannelTrafficHandler("", 0, context);
        Object object = new Object();
        Assertions.assertNotEquals(handler1, handler2);
        Assertions.assertEquals(handler1, Optional.of(handler1).get());
        Assertions.assertNotEquals(handler1.hashCode(), handler2.hashCode());
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(handler1);
        Assertions.assertNotEquals(handler1, object);
        channel.pipeline().addLast(handler2);
        Assertions.assertEquals(handler1, handler2);
        Assertions.assertEquals(handler1.hashCode(), handler2.hashCode());
    }
}
