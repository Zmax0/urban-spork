package com.urbanspork.client;

import com.urbanspork.common.Runtime;
import com.urbanspork.common.codec.address.MaybeResolved;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.protocol.dns.DnsRequest;
import com.urbanspork.common.protocol.dns.IpResponse;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.server.Server;
import com.urbanspork.test.SslUtil;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.concurrent.TimeUnit;

class ClientDnsRelayHandlerTest {
    @Test
    void testNewQuicChannelStreamFailed() throws Exception {
        Runtime runtime = new Runtime();
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setHost(InetAddress.getLoopbackAddress().getHostName());
        config.setProtocol(Protocol.trojan);
        config.setTransport(new Transport[]{Transport.QUIC});
        config.setSsl(SslUtil.getSslSetting());
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        Server.Instances instances = Server.launch(runtime, List.of(config));
        Server.Instance server = instances.servers().getFirst();
        InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), (server.udp().orElseThrow().localAddress()).getPort());
        config.setPort(serverAddress.getPort());
        ClientChannelContext context = new ClientChannelContext(config, null, null);
        Channel endpoint = context.quicEndpoint(group).syncUninterruptibly().channel();
        Promise<Channel> channelPromise = group.next().newPromise();
        ClosedChannelException cause = new ClosedChannelException();
        new QuicDnsRelayHandler(cause).newQuicChannel(
            group, serverAddress,
            new DnsRequest(InetSocketAddress.createUnresolved("127.0.0.1", 80), new Object()), context, channelPromise
        );
        Assertions.assertTrue(channelPromise.await(10, TimeUnit.SECONDS));
        Assertions.assertFalse(channelPromise.isSuccess());
        Assertions.assertSame(cause, channelPromise.cause());
        endpoint.close();
        runtime.close();
        group.shutdownGracefully();
    }

    private record QuicDnsRelayHandler(Throwable cause) implements ClientDnsRelayHandler {

        @Override
            public String label() {
                return "test";
            }

            @Override
            public Future<? extends Channel> newChannel(DnsRequest dohRequest, Promise<IpResponse> ipPromise) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isReadyOnceConnected() {
                return true;
            }

            @Override
            public void callback1(MaybeResolved maybeResolved) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void callback0(Throwable t) {
                throw new IllegalStateException(t);
            }

            @Override
            public Future<QuicStreamChannel> createQuicStreamChannel(QuicChannel quicChannel, MaybeResolved maybeResolved, ClientChannelContext ctx) {
                Promise<QuicStreamChannel> failed = ImmediateEventExecutor.INSTANCE.newPromise();
                failed.setFailure(cause);
                return failed;
            }
        }
}
