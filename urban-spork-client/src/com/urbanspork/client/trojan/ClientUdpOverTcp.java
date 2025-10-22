package com.urbanspork.client.trojan;

import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.protocol.trojan.Trojan;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

interface ClientUdpOverTcp {
    default Object convertToWrite(DatagramPacketWrapper msg) {
        DatagramPacket packet = msg.packet();
        ByteBuf buffer = Unpooled.buffer();
        Address.encode(msg.server(), buffer);
        ByteBuf content = packet.content();
        buffer.writeShort(content.readableBytes());
        buffer.writeBytes(Trojan.CRLF);
        buffer.writeBytes(content);
        return buffer;
    }

    default InetSocketAddress getKey(DatagramPacketWrapper msg) {
        return msg.packet().sender();
    }

    class InboundHandler extends ByteToMessageDecoder {
        private static final Logger logger = LoggerFactory.getLogger(InboundHandler.class);
        private final Channel channel;
        private final InetSocketAddress sender;

        InboundHandler(Channel channel, InetSocketAddress sender) {
            this.channel = channel;
            this.sender = sender;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            Channel outboundChannel = ctx.channel();
            InetSocketAddress address = Address.decode(in);
            logger.info("[udp][trojan]{}←{}~{}←{}", sender, address, channel.localAddress(), outboundChannel.localAddress());
            short length = in.readShort();
            in.skipBytes(Trojan.CRLF.length);
            ByteBuf content = in.readBytes(length);
            channel.writeAndFlush(new DatagramPacketWrapper(new DatagramPacket(content, address), sender));
        }
    }
}
