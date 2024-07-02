package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TcpRelayCodecTest {
    @Test
    void testCaughtOtherException() {
        ServerConfig config = ServerConfigTest.testConfig(0);
        TcpRelayCodec codec = new TcpRelayCodec(new Context(), config, Mode.Server);
        EmbeddedChannel channel = new EmbeddedChannel(codec);
        codec.exceptionCaught(channel.pipeline().context(codec), new RuntimeException());
        Assertions.assertThrows(RuntimeException.class, channel::checkException);
    }
}
