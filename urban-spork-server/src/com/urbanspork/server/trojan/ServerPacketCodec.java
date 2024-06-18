package com.urbanspork.server.trojan;

import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.protocol.trojan.Trojan;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.ByteToMessageCodec;

import java.net.InetSocketAddress;
import java.util.List;

class ServerPacketCodec extends ByteToMessageCodec<DatagramPacketWrapper> {
    private final InetSocketAddress address;

    ServerPacketCodec(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacketWrapper msg, ByteBuf out) {
        DatagramPacket packet = msg.packet();
        Address.encode(packet.recipient(), out);
        ByteBuf content = packet.content();
        out.writeShort(content.readableBytes());
        out.writeBytes(Trojan.CRLF);
        out.writeBytes(content);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        InetSocketAddress recipient = Address.decode(msg);
        short length = msg.readShort();
        msg.skipBytes(Trojan.CRLF.length);
        ByteBuf content = msg.readBytes(length);
        out.add(new DatagramPacket(content, recipient, address));
    }
}
