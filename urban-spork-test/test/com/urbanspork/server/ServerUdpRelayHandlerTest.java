package com.urbanspork.server;

import com.urbanspork.common.transport.udp.PacketEncoding;
import com.urbanspork.test.TestDice;
import io.netty.bootstrap.Bootstrap;
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
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

class ServerUdpRelayHandlerTest {
    @Test
    void testBindFailed() throws Exception {
        EmbeddedChannel inboundChannel = new EmbeddedChannel();
        ServerUdpRelayHandler handler = new ServerUdpRelayHandler(PacketEncoding.Packet);
        Promise<Channel> promise = handler.workerChannel("key", inboundChannel);
        Assertions.assertTrue(promise.await(5, TimeUnit.SECONDS));
        Assertions.assertFalse(promise.isSuccess());
        Assertions.assertInstanceOf(IllegalStateException.class, promise.cause());
        inboundChannel.finishAndReleaseAll();
    }

    @ParameterizedTest
    @EnumSource(PacketEncoding.class)
    void testWorkAndIdle(PacketEncoding packetEncoding) throws Exception {
        ServerUdpRelayHandler handler = new ServerUdpRelayHandler(packetEncoding);
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        Channel inboundChannel = new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(handler).bind(0).sync().channel();
        handler.workerChannel(new InetSocketAddress(TestDice.rollPort()), inboundChannel);
        InetSocketAddress recipient = new InetSocketAddress(TestDice.rollPort());
        Channel outboundChannel = handler.workerChannel(recipient, inboundChannel).get();
        Assertions.assertNotNull(outboundChannel);
        ChannelPipeline workerPipeline = outboundChannel.pipeline();
        ChannelInboundHandlerAdapter last = (ChannelInboundHandlerAdapter) workerPipeline.last();
        ChannelHandlerContext ctx = workerPipeline.lastContext();
        DatagramPacket msg = new DatagramPacket(
            Unpooled.wrappedBuffer(TestDice.rollString().getBytes()),
            new InetSocketAddress(0), new InetSocketAddress(0)
        );
        last.channelRead(ctx, msg);
        last.userEventTriggered(ctx, new Object());
        last.userEventTriggered(ctx, IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT);
        outboundChannel.closeFuture().await();
        Assertions.assertFalse(outboundChannel.isActive());
        outboundChannel.close();
        inboundChannel.close();
        group.shutdownGracefully();
    }
}
