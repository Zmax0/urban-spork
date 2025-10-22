package com.urbanspork.client;

import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.test.SslUtil;
import com.urbanspork.test.TestDice;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

class ClientUdpDnsDecoderTest {
    @Test
    void testError() {
        String str = TestDice.rollString();
        DnsSetting dnsSetting = new DnsSetting(String.format("https://localhost:%d/dns-query", TestDice.rollPort()), null);
        Protocol protocol = Protocol.trojan;
        String password = TestDice.rollPassword(protocol, null);
        ServerConfig config = ServerConfigTest.testConfig(TestDice.rollPort());
        config.setTransport(new Transport[]{Transport.UDP});
        config.setProtocol(protocol);
        config.setPassword(password);
        config.setSsl(SslUtil.getSslSetting());
        config.setDns(dnsSetting);
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(str.getBytes()), new InetSocketAddress(0));
        DatagramPacketWrapper msg = new DatagramPacketWrapper(data, InetSocketAddress.createUnresolved(TestDice.rollHost(), TestDice.rollPort()));
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        // tcp
        Promise<Object> p1 = group.next().newPromise();
        EmbeddedChannel ch1 = newChannel(config, group, p1);
        ch1.writeOneInbound(msg);
        Assertions.assertThrows(ExecutionException.class, p1::get);
        // quic
        Promise<Object> p2 = group.next().newPromise();
        config.setTransport(new Transport[]{Transport.QUIC});
        EmbeddedChannel ch2 = newChannel(config, group, p2);
        ch2.writeOneInbound(msg);
        Assertions.assertThrows(ExecutionException.class, p2::get);
    }

    private static EmbeddedChannel newChannel(ServerConfig serverConfig, EventLoopGroup group, Promise<Object> promise) {
        return new EmbeddedChannel(
            new ClientUdpDnsDecoder(new ClientChannelContext(serverConfig, null, null), group),
            new SimpleChannelInboundHandler<DatagramPacketWrapper>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, DatagramPacketWrapper msg) {
                    promise.setSuccess(msg);
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    promise.setFailure(cause);
                }
            }
        );
    }
}
