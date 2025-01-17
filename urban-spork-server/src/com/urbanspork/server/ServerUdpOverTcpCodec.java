package com.urbanspork.server;

import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;

import java.net.InetSocketAddress;
import java.util.List;

class ServerUdpOverTcpCodec extends MessageToMessageCodec<ByteBuf, DatagramPacketWrapper> {

    private final InetSocketAddress address;

    ServerUdpOverTcpCodec(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacketWrapper msg, List<Object> out) {
        msg.retain();
        out.add(msg.packet().content());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        Channel channel = ctx.channel();
        if (channel.remoteAddress() instanceof InetSocketAddress sender) {
            out.add(new DatagramPacket(msg.retain(), address, sender));
        } else {
            out.add(new DatagramPacket(msg.retain(), address));
        }
    }
}
