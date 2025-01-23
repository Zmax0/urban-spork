package com.urbanspork.client.vmess;

import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

interface ClientUdpOverTcp {
    default Object convertToWrite(DatagramPacketWrapper msg) {
        return msg.packet().content();
    }

    default Key getKey(DatagramPacketWrapper msg) {
        return new Key(msg.packet().sender(), msg.proxy() /* recipient */);
    }

    class InboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private static final Logger logger = LoggerFactory.getLogger(InboundHandler.class);
        private final Channel channel;
        private final InetSocketAddress sender;
        private final InetSocketAddress recipient;

        InboundHandler(Channel channel, InetSocketAddress recipient, InetSocketAddress sender) {
            super(false);
            this.channel = channel;
            this.recipient = recipient;
            this.sender = sender;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            Channel inboundChannel = ctx.channel();
            logger.info("[udp][vmess]{} ← {} ~ {} ← {}", sender, inboundChannel.localAddress(), inboundChannel.remoteAddress(), recipient);
            channel.writeAndFlush(new DatagramPacketWrapper(new DatagramPacket(msg, recipient), sender));
        }
    }
}
