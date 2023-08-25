package com.urbanspork.common.codec.socks;

import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.socks.Address;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;

import java.util.List;

public class DatagramPacketEncoder extends MessageToMessageEncoder<TernaryDatagramPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TernaryDatagramPacket msg, List<Object> out) throws Exception {
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(new byte[]{0, 0, 0/* fragment */});
        DatagramPacket data = msg.packet();
        Address.encode(Socks5CommandType.CONNECT, data.recipient(), buffer);
        buffer.writeBytes(data.content());
        out.add(new DatagramPacket(buffer, msg.third(), data.sender()));
    }
}
