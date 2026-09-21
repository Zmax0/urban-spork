package com.urbanspork.client.vmess;

import com.urbanspork.client.ClientChannelContext;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.test.TestDice;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class ClientUdpOverTcpHandlerTest {
    @Test
    void testConnectFailed() throws InterruptedException {
        ServerConfig config = ServerConfigTest.testConfig(TestDice.rollPort());
        config.setProtocol(Protocol.vmess);
        config.setPassword(TestDice.rollPassword(Protocol.vmess, CipherKind.chacha20_poly1305));
        TestClientUdpOverTcpHandler handler = new TestClientUdpOverTcpHandler(new ClientChannelContext(config, null, null));
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        DatagramPacketWrapper packet = new DatagramPacketWrapper(new DatagramPacket(Unpooled.EMPTY_BUFFER, new InetSocketAddress(TestDice.rollPort())), new InetSocketAddress(0));
        // a failed connect is reported to the binding promise, it must not be thrown at the caller
        Assertions.assertDoesNotThrow(() -> channel.writeInbound(packet));
        CountDownLatch failure = new CountDownLatch(1);
        handler.promise.addListener(_ -> failure.countDown());
        Assertions.assertTrue(failure.await(5, TimeUnit.SECONDS));
        Assertions.assertFalse(handler.promise.isSuccess());
        channel.close();
    }

    private static class TestClientUdpOverTcpHandler extends ClientUdpOverTcpHandler {
        private Promise<Channel> promise;

        private TestClientUdpOverTcpHandler(ClientChannelContext context) {
            super(context);
        }

        @Override
        protected void newBindingChannel(Channel inboundChannel, Key k, Promise<Channel> promise) {
            this.promise = promise;
            super.newBindingChannel(inboundChannel, k, promise);
        }
    }
}
