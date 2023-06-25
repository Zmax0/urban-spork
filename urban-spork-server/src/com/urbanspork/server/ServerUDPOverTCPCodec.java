package com.urbanspork.server;

import com.urbanspork.common.network.TernaryDatagramPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import java.net.InetSocketAddress;
import java.util.List;

class ServerUDPOverTCPCodec extends MessageToMessageCodec<ByteBuf, TernaryDatagramPacket> {

    private final Socks5CommandRequest request;

    ServerUDPOverTCPCodec(Socks5CommandRequest request) {
        this.request = request;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, TernaryDatagramPacket msg, List<Object> out) {
        out.add(msg.packet().content());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        InetSocketAddress sender = (InetSocketAddress) ctx.channel().remoteAddress();
        out.add(new DatagramPacket(msg.retainedDuplicate(), new InetSocketAddress(request.dstAddr(), request.dstPort()), sender));
    }
}
