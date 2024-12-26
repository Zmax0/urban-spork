package com.urbanspork.client.shadowsocks;

import com.urbanspork.client.AbstractClientUdpRelayHandler;
import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.udp.UdpRelayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;

public class ClientUdpRelayHandler extends AbstractClientUdpRelayHandler<InetSocketAddress> {

    private static final Logger logger = LoggerFactory.getLogger(ClientUdpRelayHandler.class);
    private final EventLoopGroup workerGroup;
    private final InetSocketAddress relay;
    private final UdpRelayCodec codec; // used by outbound (client-server) channel and its lifetime controlled by inbound (local-client) channel

    public ClientUdpRelayHandler(ServerConfig config, EventLoopGroup workerGroup) {
        super(config, Duration.ofMinutes(10));
        this.workerGroup = workerGroup;
        this.relay = new InetSocketAddress(config.getHost(), config.getPort());
        UdpRelayCodec codec = new UdpRelayCodec(config, Mode.Client, ServerUserManager.empty());
        codec.setAutoRelease(false);
        this.codec = codec;
    }

    @Override
    protected Object convertToWrite(DatagramPacketWrapper msg) {
        DatagramPacket packet = msg.packet();
        return new DatagramPacketWrapper(new DatagramPacket(packet.content(), msg.proxy(), packet.sender()), relay);
    }

    @Override
    protected InetSocketAddress getKey(DatagramPacketWrapper msg) {
        return msg.packet().sender();
    }

    @Override
    protected Channel newBindingChannel(Channel inboundChannel, InetSocketAddress sender) {
        return new Bootstrap().group(workerGroup).channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        codec,
                        new InboundHandler(inboundChannel, sender),// server->client->sender
                        new ExceptionHandler(config)
                    );
                }
            }).bind(0) // automatically assigned port now, may have security implications
            .syncUninterruptibly().channel();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        super.handlerRemoved(ctx);
        codec.release();
    }

    private static class InboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        private final Channel channel;
        private final InetSocketAddress sender;

        InboundHandler(Channel channel, InetSocketAddress sender) {
            super(false);
            this.channel = channel;
            this.sender = sender;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            logger.info("[udp][shadowsocks]{}←{}~{}←{}", sender, packet.recipient(), ctx.channel().localAddress(), packet.sender());
            channel.writeAndFlush(new DatagramPacketWrapper(new DatagramPacket(packet.content(), packet.recipient()), sender));
        }
    }
}
