package com.urbanspork.client;

import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.test.SslUtil;
import com.urbanspork.test.TestDice;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class ClientUdpDnsDecoderTest {
    @Test
    void testError() {
        assertErrorCaught(new Transport[]{Transport.UDP});
    }

    @Test
    void testQuicError() {
        assertErrorCaught(new Transport[]{Transport.QUIC});
    }

    private static void assertErrorCaught(Transport[] transport) {
        String str = TestDice.rollString();
        DnsSetting dnsSetting = new DnsSetting(String.format("https://localhost:%d/dns-query", TestDice.rollPort()), null);
        Protocol protocol = Protocol.trojan;
        String password = TestDice.rollPassword(protocol, null);
        ServerConfig config = ServerConfigTest.testConfig(closedPort());
        config.setTransport(transport);
        config.setProtocol(protocol);
        config.setPassword(password);
        config.setSsl(SslUtil.getSslSetting());
        config.setDns(dnsSetting);
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(str.getBytes()), new InetSocketAddress(0));
        DatagramPacketWrapper msg = new DatagramPacketWrapper(data, InetSocketAddress.createUnresolved(TestDice.rollHost(), TestDice.rollPort()));
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            Promise<Object> promise = group.next().newPromise();
            newChannel(config, group, promise).pipeline().fireChannelRead(msg);
            Assertions.assertThrows(ExecutionException.class, () -> promise.get(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Assertions.fail(e);
        } finally {
            group.shutdownGracefully();
        }
    }

    private static Channel newChannel(ServerConfig serverConfig, EventLoopGroup group, Promise<Object> promise) throws InterruptedException {
        return new Bootstrap()
            .group(group)
            .channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new ClientUdpDnsDecoder(new ClientChannelContext(serverConfig, null, null)));
                    ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacketWrapper>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacketWrapper msg) {
                            promise.setSuccess(msg);
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                            promise.setFailure(cause);
                        }
                    });
                }
            })
            .bind(0)
            .sync()
            .channel();
    }

    private static int closedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
