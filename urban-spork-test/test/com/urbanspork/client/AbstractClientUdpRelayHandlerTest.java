package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.test.TestDice;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

class AbstractClientUdpRelayHandlerTest {
    @Test
    void test() throws SSLException {
        EmbeddedChannel outbound = new EmbeddedChannel();
        EmbeddedChannel inbound = new EmbeddedChannel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        ClientChannelContext context = new ClientChannelContext(config, null, null);
        inbound.pipeline().addLast(new TestClientUdpRelayHandler(context, outbound));
        InetSocketAddress recipient = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        DatagramPacketWrapper msg = new DatagramPacketWrapper(new DatagramPacket(Unpooled.wrappedBuffer(TestDice.rollString(10).getBytes()), recipient), recipient);
        Assertions.assertFalse(inbound.writeInbound(msg));
        Assertions.assertFalse(inbound.writeInbound(msg));
        outbound.close();
    }

    @Test
    void bindingFailureReleasesInbound() throws SSLException {
        EmbeddedChannel inbound = new EmbeddedChannel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        ClientChannelContext context = new ClientChannelContext(config, null, null);
        inbound.pipeline().addLast(new FailingClientUdpRelayHandler(context));
        InetSocketAddress recipient = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        DatagramPacketWrapper msg = new DatagramPacketWrapper(new DatagramPacket(Unpooled.copiedBuffer("payload", StandardCharsets.US_ASCII), recipient), recipient);
        Assertions.assertDoesNotThrow(() -> inbound.writeInbound(msg));
        Assertions.assertEquals(0, msg.refCnt());
        inbound.close();
    }

    private static class TestClientUdpRelayHandler extends AbstractClientUdpRelayHandler<String, Channel> {
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
        protected void newBindingChannel(Channel inboundChannel, String s, Promise<Channel> promise) {
            promise.trySuccess(outbound);
        }
    }

    private static class FailingClientUdpRelayHandler extends AbstractClientUdpRelayHandler<String, Channel> {
        protected FailingClientUdpRelayHandler(ClientChannelContext context) {
            super(context, Duration.ofSeconds(1));
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
        protected void newBindingChannel(Channel inboundChannel, String key, Promise<Channel> promise) {
            promise.tryFailure(new IllegalStateException("bind failed"));
        }
    }

}
