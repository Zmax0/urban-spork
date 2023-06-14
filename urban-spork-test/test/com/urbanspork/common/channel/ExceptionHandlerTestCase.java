package com.urbanspork.common.channel;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import com.urbanspork.test.TestDice;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.bouncycastle.crypto.InvalidCipherTextException;
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
        }, new ExceptionHandler(config));
        Assertions.assertTrue(channel.isActive());
        channel.writeInbound("Error message");
        Assertions.assertFalse(channel.isActive());
    }

    @Test
    void testInvalidCipherText() {
        ServerConfig config = ServerConfigTestCase.testConfig(TestDice.rollPort());
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws InvalidCipherTextException {
                throw new InvalidCipherTextException(msg.toString());
            }
        }, new ExceptionHandler(config));
        Assertions.assertTrue(channel.isActive());
        channel.writeInbound("Invalid cipher text");
        Assertions.assertFalse(channel.isActive());
    }
}
