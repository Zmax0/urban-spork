package com.urbanspork.common.protocol.socks;

import com.urbanspork.common.network.TernaryDatagramPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class Socks5DatagramPacketEncoder extends MessageToMessageEncoder<TernaryDatagramPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TernaryDatagramPacket msg, List<Object> out) throws Exception {
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(new byte[]{0, 0, 0/* Fragment */});
        DatagramPacket data = msg.packet();
        Socks5Addressing.encode(data.recipient(), buffer);
        buffer.writeBytes(data.content());
        out.add(new DatagramPacket(buffer, msg.third(), data.sender()));
    }
}
