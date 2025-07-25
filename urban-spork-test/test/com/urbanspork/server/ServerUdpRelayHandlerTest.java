package com.urbanspork.server;

import com.urbanspork.common.transport.udp.PacketEncoding;
import com.urbanspork.test.TestDice;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.InetSocketAddress;

class ServerUdpRelayHandlerTest {
    @ParameterizedTest
    @EnumSource(PacketEncoding.class)
    void testWorkAndIdle(PacketEncoding packetEncoding) throws Exception {
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        ServerUdpRelayHandler handler = new ServerUdpRelayHandler(packetEncoding, group);
        handler.workerChannel(new InetSocketAddress(TestDice.rollPort()), new EmbeddedChannel(handler));
        InetSocketAddress recipient = new InetSocketAddress(TestDice.rollPort());
        Channel outboundChannel = handler.workerChannel(recipient, new EmbeddedChannel(new ServerUdpRelayHandler(packetEncoding, group)));
        Assertions.assertNotNull(outboundChannel);
        ChannelPipeline workerPipeline = outboundChannel.pipeline();
        ChannelInboundHandlerAdapter last = (ChannelInboundHandlerAdapter) workerPipeline.last();
        ChannelHandlerContext ctx = workerPipeline.lastContext();
        last.channelRead(ctx, new DatagramPacket(Unpooled.wrappedBuffer(TestDice.rollString().getBytes()), new InetSocketAddress(0), new InetSocketAddress(0)));
        last.userEventTriggered(ctx, new Object());
        last.userEventTriggered(ctx, IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT);
        outboundChannel.closeFuture().await();
        Assertions.assertFalse(outboundChannel.isActive());
    }
}
