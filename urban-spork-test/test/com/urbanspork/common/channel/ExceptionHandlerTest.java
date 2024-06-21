package com.urbanspork.common.channel;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.test.TestDice;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExceptionHandlerTest {
    @Test
    void testCaughtException() {
        ServerConfig config = ServerConfigTest.testConfig(TestDice.rollPort());
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                ExceptionHandler handler = ctx.pipeline().get(ExceptionHandler.class);
                ctx.close();
                handler.exceptionCaught(ctx, new UnsupportedOperationException(msg.toString()));
            }
        }, new ExceptionHandler(config));
        Assertions.assertTrue(channel.isActive());
        channel.writeInbound("Test");
        Assertions.assertFalse(channel.isActive());
    }
}
