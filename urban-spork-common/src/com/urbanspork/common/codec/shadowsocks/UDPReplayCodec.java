package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.socks.Address;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class UDPReplayCodec extends MessageToMessageCodec<DatagramPacket, TernaryDatagramPacket> {

    private final AEADCipherCodec cipher;

    public UDPReplayCodec(ServerConfig config) {
        this.cipher = AEADCipherCodecs.get(config.getPassword(), config.getCipher(), Network.UDP);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, TernaryDatagramPacket msg, List<Object> out) throws Exception {
        InetSocketAddress proxy = msg.third();
        if (proxy == null) {
            throw new EncoderException("Replay address is null");
        }
        ByteBuf in = ctx.alloc().buffer();
        DatagramPacket data = msg.packet();
        Address.encode(Socks5CommandType.CONNECT, data.recipient(), in);
        in.writeBytes(data.content().duplicate());
        ByteBuf content = ctx.alloc().buffer();
        cipher.encode(ctx, in, content);
        out.add(new DatagramPacket(content, proxy, data.sender()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        List<Object> list = new ArrayList<>(1);
        cipher.decode(ctx, msg.content(), list);
        ByteBuf in = (ByteBuf) list.get(0);
        InetSocketAddress recipient = Address.decode(in);
        out.add(new DatagramPacket(in.retainedDuplicate(), recipient, msg.sender()));
    }
}