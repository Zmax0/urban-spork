package com.urbanspork.common.protocol.socks;

import com.urbanspork.common.network.TernaryDatagramPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.net.InetSocketAddress;
import java.util.List;

public class DatagramPacketDecoder extends MessageToMessageDecoder<DatagramPacket> {
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        ByteBuf content = msg.content();
        if (content.readableBytes() < 5) {
            ctx.fireExceptionCaught(new IllegalArgumentException("Insufficient length of packet"));
            return;
        }
        if (content.getByte(2) != 0) {
            ctx.fireExceptionCaught(new IllegalArgumentException("Discarding fragmented payload"));
            return;
        }
        content.skipBytes(3);
        InetSocketAddress address = Address.decode(content);
        out.add(new TernaryDatagramPacket(msg.replace(content.retainedDuplicate()), address));
    }
}