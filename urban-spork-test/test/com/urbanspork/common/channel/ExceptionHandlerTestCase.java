package com.urbanspork.common.channel;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import com.urbanspork.test.TestDice;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Common - Exception Handler")
class ExceptionHandlerTestCase {
    @Test
    void testCaughtException() {
        ServerConfig config = ServerConfigTestCase.testConfig(TestDice.rollPort());
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                throw new UnsupportedOperationException(msg.toString());
            }
        }, new ExceptionHandler(config, Mode.Client));
        Assertions.assertTrue(channel.isActive());
        channel.writeInbound("Testcase");
        Assertions.assertFalse(channel.isActive());
    }
}
