package com.urbanspork.common.protocol.socks.udp;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.protocol.socks.Socks5Addressing;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.net.InetSocketAddress;
import java.util.List;

public class Socks5DatagramPacketEncoder extends MessageToMessageEncoder<DatagramPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        InetSocketAddress request = ctx.channel().attr(AttributeKeys.SOCKS5_DST_ADDR).get();
        if (request == null) {
            ctx.fireExceptionCaught(new NullPointerException("Desired destination address is null."));
            return;
        }
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(new byte[]{0, 0, 0/* Fragment */});
        Socks5Addressing.encode(request, buffer);
        buffer.writeBytes(msg.content());
        out.add(msg.replace(buffer));
    }

}
