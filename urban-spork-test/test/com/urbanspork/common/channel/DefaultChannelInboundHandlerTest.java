package com.urbanspork.common.channel;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultChannelInboundHandlerTest {
    @Test
    void testExceptionCaught() {
        EmbeddedChannel channel1 = new EmbeddedChannel();
        DefaultChannelInboundHandler handler = new DefaultChannelInboundHandler(channel1);
        EmbeddedChannel channel2 = new EmbeddedChannel(handler);
        handler.exceptionCaught(channel2.pipeline().lastContext(), new UnsupportedOperationException("Test"));
        Assertions.assertFalse(channel1.isActive());
    }
}
