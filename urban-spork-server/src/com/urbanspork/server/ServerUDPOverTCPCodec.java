package com.urbanspork.server;

import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;

import java.net.InetSocketAddress;
import java.util.List;

class ServerUDPOverTCPCodec extends MessageToMessageCodec<ByteBuf, DatagramPacketWrapper> {

    private final InetSocketAddress address;

    ServerUDPOverTCPCodec(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacketWrapper msg, List<Object> out) {
        out.add(msg.packet().content());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        InetSocketAddress sender = (InetSocketAddress) ctx.channel().remoteAddress();
        out.add(new DatagramPacket(msg.retainedDuplicate(), address, sender));
    }
}
