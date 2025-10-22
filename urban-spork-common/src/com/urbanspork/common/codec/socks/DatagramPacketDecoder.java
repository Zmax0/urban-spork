package com.urbanspork.common.codec.socks;

import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.net.InetSocketAddress;
import java.util.List;

public class DatagramPacketDecoder extends MessageToMessageDecoder<DatagramPacket> {
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
        ByteBuf content = msg.content();
        if (content.readableBytes() < 5) {
            throw new DecoderException("insufficient length of packet");
        }
        if (content.getByte(2) != 0) {
            throw new DecoderException("discarding fragmented payload");
        }
        content.skipBytes(3);
        InetSocketAddress server = Address.decode(content);
        out.add(new DatagramPacketWrapper(msg.replace(content).retain(), server));
    }
}
