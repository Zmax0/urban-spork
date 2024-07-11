package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfigTest;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TcpRelayCodecTest {
    @Test
    void testCaughtOtherException() {
        testCaughtOtherException(Mode.Server, new RuntimeException());
        testCaughtOtherException(Mode.Client, new DecoderException());
    }

    void testCaughtOtherException(Mode mode, Throwable throwable) {
        TcpRelayCodec codec = new TcpRelayCodec(new Context(), ServerConfigTest.testConfig(0), mode);
        EmbeddedChannel channel = new EmbeddedChannel(codec);
        codec.exceptionCaught(channel.pipeline().context(codec), throwable);
        Assertions.assertThrows(throwable.getClass(), channel::checkException);
    }
}
