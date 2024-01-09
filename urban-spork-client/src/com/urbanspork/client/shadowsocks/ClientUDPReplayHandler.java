package com.urbanspork.client.shadowsocks;

import com.urbanspork.client.AbstractClientUDPReplayHandler;
import com.urbanspork.common.channel.ExceptionHandler;
import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.codec.shadowsocks.UDPReplayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class ClientUDPReplayHandler extends AbstractClientUDPReplayHandler<InetSocketAddress> {

    private static final Logger logger = LoggerFactory.getLogger(ClientUDPReplayHandler.class);
    private final EventLoopGroup workerGroup;
    private final InetSocketAddress replay;

    public ClientUDPReplayHandler(ServerConfig config, EventLoopGroup workerGroup) {
        super(config);
        this.workerGroup = workerGroup;
        this.replay = new InetSocketAddress(config.getHost(), config.getPort());
    }

    @Override
    protected Object convertToWrite(TernaryDatagramPacket msg) {
        return new TernaryDatagramPacket(new DatagramPacket(msg.packet().content(), msg.third()), replay);
    }

    @Override
    protected InetSocketAddress getKey(TernaryDatagramPacket msg) {
        return msg.packet().sender();
    }

    @Override
    protected Channel newBindingChannel(Channel inboundChannel, InetSocketAddress sender) {
        return new Bootstrap().group(workerGroup).channel(NioDatagramChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(
                        new UDPReplayCodec(config, Mode.Client),
                        new InboundHandler(inboundChannel, sender),// server->client->sender
                        new ExceptionHandler(config)
                    );
                }
            }).bind(0) // automatically assigned port now, may have security implications
            .syncUninterruptibly().channel();
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
            channel.writeAndFlush(new TernaryDatagramPacket(new DatagramPacket(packet.content(), packet.recipient()), sender));
        }
    }
}
