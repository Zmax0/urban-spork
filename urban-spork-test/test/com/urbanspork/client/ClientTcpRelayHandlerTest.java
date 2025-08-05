package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.test.SslUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

class ClientTcpRelayHandlerTest {
    @Test
    void testConnectQuicServerFailed() throws Exception {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        Channel inbound = new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(new ChannelInboundHandlerAdapter()).bind(0).sync().channel();
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setSsl(SslUtil.getSslSetting());
        config.setTransport(new Transport[]{Transport.QUIC});
        inbound.attr(ClientChannelContext.KEY).set(new ClientChannelContext(config, null, null));
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
        testHandler.connect(inbound, InetSocketAddress.createUnresolved("localhost", 0));
        Boolean res = promise.get();
        Assertions.assertFalse(res);
    }
}
