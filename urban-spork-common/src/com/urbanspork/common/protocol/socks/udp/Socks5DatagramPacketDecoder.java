package com.urbanspork.common.protocol.socks.udp;

import com.urbanspork.common.protocol.socks.Socks5Addressing;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.net.InetSocketAddress;
import java.util.List;

public class Socks5DatagramPacketDecoder extends MessageToMessageDecoder<DatagramPacket> {
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
        out.add(new Result(Socks5Addressing.decode(content), msg.replace(content.retainedSlice())));
    }

    public record Result(InetSocketAddress dstAddr, DatagramPacket data) {
    }

}
