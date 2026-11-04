package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.test.SslUtil;
import com.urbanspork.test.TestDice;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

class AbstractClientUdpOverQuicHandlerTest {
    private static final String KEY = "key";

    @Test
    void testEndpointFailed() throws Exception {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        IllegalStateException cause = new IllegalStateException("endpoint is not active");
        Promise<QuicStreamChannel> promise = group.next().newPromise();
        new TestClientUdpOverQuicHandler(context(), group) {
            @Override
            protected ChannelFuture newEndpoint(ClientChannelContext context, EventLoopGroup workerGroup) {
                return endpoint(workerGroup).channel().newFailedFuture(cause);
            }
        }.newBindingChannel(new LocalChannel(), KEY, promise);
        Assertions.assertTrue(promise.await(5, TimeUnit.SECONDS));
        Assertions.assertFalse(promise.isSuccess());
        Assertions.assertSame(cause, promise.cause());
        group.shutdownGracefully().syncUninterruptibly();
    }

    @Test
    void testConnectServerFailed() throws Exception {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        IllegalStateException cause = new IllegalStateException("connect relay server failed");
        TestClientUdpOverQuicHandler handler = new TestClientUdpOverQuicHandler(context(), group) {
            @Override
            protected ChannelFuture newEndpoint(ClientChannelContext context, EventLoopGroup workerGroup) {
                return endpoint(workerGroup);
            }

            @Override
            protected Future<QuicChannel> newQuicChannel(Channel endpointChannel, InetSocketAddress serverAddress) {
                Promise<QuicChannel> failed = ImmediateEventExecutor.INSTANCE.newPromise();
                failed.setFailure(cause);
                return failed;
            }
        };
        try {
            Promise<QuicStreamChannel> promise = group.next().newPromise();
            handler.newBindingChannel(new LocalChannel(), KEY, promise);
            Assertions.assertTrue(promise.await(5, TimeUnit.SECONDS));
            Assertions.assertFalse(promise.isSuccess());
            Assertions.assertSame(cause, promise.cause());
            Assertions.assertFalse(handler.endpoint.channel().isOpen());
        } finally {
            group.shutdownGracefully().syncUninterruptibly();
        }
    }

    private static ChannelFuture endpoint(EventLoopGroup workerGroup) {
        NioSocketChannel channel = new NioSocketChannel();
        return workerGroup.register(channel);
    }

    private static ClientChannelContext context() throws Exception {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setHost(InetAddress.getLoopbackAddress().getHostName());
        config.setProtocol(Protocol.trojan);
        config.setTransport(new Transport[]{Transport.QUIC});
        config.setSsl(SslUtil.getSslSetting());
        config.setPort(TestDice.rollPort());
        return new ClientChannelContext(config, null, null);
    }

    private static class TestClientUdpOverQuicHandler extends AbstractClientUdpOverQuicHandler<String> {
        private TestClientUdpOverQuicHandler(ClientChannelContext context, EventLoopGroup group) {
            super(context, Duration.ofSeconds(1), group);
        }

        @Override
        protected ChannelInitializer<Channel> newOutboundInitializer(String k) {
            return new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                }
            };
        }

        @Override
        protected ChannelHandler newInboundHandler(Channel inbound, String k) {
            return new ChannelInboundHandlerAdapter();
        }

        @Override
        protected Object convertToWrite(DatagramPacketWrapper msg) {
            return msg;
        }

        @Override
        protected String getKey(DatagramPacketWrapper msg) {
            return KEY;
        }
    }
}
