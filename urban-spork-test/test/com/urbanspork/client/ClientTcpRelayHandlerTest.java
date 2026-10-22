package com.urbanspork.client;

import com.urbanspork.common.Runtime;
import com.urbanspork.common.codec.address.MaybeResolved;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.server.Server;
import com.urbanspork.test.SslUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class ClientTcpRelayHandlerTest {
    @Test
    void testConnectQuicServerFailed() throws Exception {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        Channel inbound = new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(new ChannelInboundHandlerAdapter()).bind(0).sync().channel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setSsl(SslUtil.getSslSetting());
        config.setTransport(new Transport[]{Transport.QUIC});
        Promise<Boolean> promise = group.next().newPromise();
        ClientRelayHandler.InboundReady inboundReady = new ClientRelayHandler.InboundReady(
            _ -> promise.setSuccess(true), _ -> promise.setSuccess(false)
        );
        ClientTcpRelayHandler testHandler = new ClientTcpRelayHandler() {
            @Override
            public InboundReady inboundReady() {
                return inboundReady;
            }
        };
        testHandler.connect(inbound, InetSocketAddress.createUnresolved("localhost", 0), new ClientChannelContext(config, null, null));
        Boolean res = promise.get();
        Assertions.assertFalse(res);
        group.shutdownGracefully().sync();
    }

    @Test
    void testConnectQuicStreamChannelFailed() throws Exception {
        Runtime runtime = new Runtime();
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setSsl(SslUtil.getSslSetting());
        config.setTransport(new Transport[]{Transport.QUIC});
        Server.Instances instances = Server.launch(runtime, List.of(config));
        Server.Instance server = instances.servers().getFirst();
        int serverPort = (server.udp().orElseThrow().localAddress()).getPort();
        config.setPort(serverPort);
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        Channel inbound = new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(new ChannelInboundHandlerAdapter()).bind(0).sync().channel();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<MaybeResolved> resolved = new AtomicReference<>();
        ClientTcpRelayHandler testHandler = newTestHandler(resolved, countDownLatch);
        ClientChannelContext context = new ClientChannelContext(config, null, null);
        testHandler.connect(inbound, InetSocketAddress.createUnresolved("localhost", 80), context);
        Assertions.assertTrue(countDownLatch.await(10, TimeUnit.SECONDS), "stream channel creation should be reached");
        Assertions.assertEquals(80, resolved.get().original().getPort());
        group.shutdownGracefully().sync();
        runtime.close();
    }

    private static ClientTcpRelayHandler newTestHandler(AtomicReference<MaybeResolved> resolved, CountDownLatch streamAttempt) {
        ClosedChannelException cause = new ClosedChannelException();
        return new ClientTcpRelayHandler() {
            @Override
            public Future<QuicStreamChannel> createQuicStreamChannel(QuicChannel quicChannel, MaybeResolved dstAddress, ClientChannelContext ctx) {
                resolved.set(dstAddress);
                Promise<QuicStreamChannel> failed = ImmediateEventExecutor.INSTANCE.newPromise();
                failed.setFailure(cause);
                streamAttempt.countDown();
                return failed;
            }
        };
    }
}
