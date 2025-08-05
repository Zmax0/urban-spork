package com.urbanspork.common.codec.socks;

import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class DatagramPacketEncoder extends MessageToMessageEncoder<DatagramPacketWrapper> {
    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacketWrapper msg, List<Object> out) {
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(new byte[]{0, 0, 0/* fragment */});
        DatagramPacket data = msg.packet();
        Address.encode(data.recipient(), buffer);
        buffer.writeBytes(data.content());
        out.add(new DatagramPacket(buffer, msg.server(), data.sender()));
    }
}
