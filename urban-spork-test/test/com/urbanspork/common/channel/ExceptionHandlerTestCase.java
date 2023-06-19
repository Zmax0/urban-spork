package com.urbanspork.common.channel;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import com.urbanspork.test.TestDice;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        channel.pipeline().addLast(new MessageToMessageDecoder<String>() {
            @Override
            protected void decode(ChannelHandlerContext ctx, String in, List<Object> out) throws InvalidCipherTextException {
                throw new InvalidCipherTextException(in);
            }
        }, new ExceptionHandler(config));
        Assertions.assertTrue(channel.isActive());
        channel.writeInbound("Invalid cipher text");
        Assertions.assertFalse(channel.isActive());
    }
}
