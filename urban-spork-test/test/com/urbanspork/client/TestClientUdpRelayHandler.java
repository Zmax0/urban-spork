package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import com.urbanspork.test.TestDice;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@DisplayName("Client - AbstractClientUdpRelayHandler")
class AbstractClientUdpRelayHandlerTestCase {
    @Test
    void test() {
        EmbeddedChannel outbound = new EmbeddedChannel();
        EmbeddedChannel inbound = new EmbeddedChannel();
        ServerConfig config = ServerConfigTestCase.testConfig(0);
        inbound.pipeline().addLast(new TestClientUdpRelayHandler(config, outbound));
        InetSocketAddress recipient = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        TernaryDatagramPacket msg = new TernaryDatagramPacket(new DatagramPacket(Unpooled.wrappedBuffer(TestDice.rollString(10).getBytes()), recipient), recipient);
        Assertions.assertFalse(inbound.writeInbound(msg));
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        Assertions.assertFalse(inbound.writeInbound(msg));
        outbound.close();
    }
}

class TestClientUdpRelayHandler extends AbstractClientUdpRelayHandler<String> {
    private final Channel outbound;

    protected TestClientUdpRelayHandler(ServerConfig config, Channel outbound) {
        super(config, Duration.ofSeconds(1));
        this.outbound = outbound;
    }

    @Override
    protected Object convertToWrite(TernaryDatagramPacket msg) {
        return msg;
    }

    @Override
    protected String getKey(TernaryDatagramPacket msg) {
        return msg.packet().content().toString(StandardCharsets.US_ASCII);
    }

    @Override
    protected Channel newBindingChannel(Channel inboundChannel, String s) {
        return outbound;
    }
}
