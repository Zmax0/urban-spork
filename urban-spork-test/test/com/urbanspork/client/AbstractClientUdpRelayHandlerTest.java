package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.test.TestDice;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

class AbstractClientUdpRelayHandlerTest {
    @Test
    void test() {
        EmbeddedChannel outbound = new EmbeddedChannel();
        EmbeddedChannel inbound = new EmbeddedChannel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        ClientChannelContext context = new ClientChannelContext(config, null, null);
        inbound.pipeline().addLast(new TestClientUdpRelayHandler(context, outbound));
        InetSocketAddress recipient = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        DatagramPacketWrapper msg = new DatagramPacketWrapper(new DatagramPacket(Unpooled.wrappedBuffer(TestDice.rollString(10).getBytes()), recipient), recipient);
        Assertions.assertFalse(inbound.writeInbound(msg));
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        Assertions.assertFalse(inbound.writeInbound(msg));
        outbound.close();
    }

    private static class TestClientUdpRelayHandler extends AbstractClientUdpRelayHandler<String> {
        private final Channel outbound;

        protected TestClientUdpRelayHandler(ClientChannelContext context, Channel outbound) {
            super(context, Duration.ofSeconds(1));
            this.outbound = outbound;
        }

        @Override
        protected Object convertToWrite(DatagramPacketWrapper msg) {
            return msg;
        }

        @Override
        protected String getKey(DatagramPacketWrapper msg) {
            return msg.packet().content().toString(StandardCharsets.US_ASCII);
        }

        @Override
        protected Channel newBindingChannel(Channel inboundChannel, String s) {
            return outbound;
        }
    }

}
