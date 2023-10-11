package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.shadowsocks.RequestHeader;
import com.urbanspork.common.protocol.socks.Socks5;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class UDPReplayCodec extends MessageToMessageCodec<DatagramPacket, TernaryDatagramPacket> {

    private final AEADCipherCodec cipher;

    public UDPReplayCodec(ServerConfig config) {
        this.cipher = AEADCipherCodecs.get(config.getPassword(), config.getCipher());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, TernaryDatagramPacket msg, List<Object> out) throws Exception {
        InetSocketAddress proxy = msg.third();
        if (proxy == null) {
            throw new EncoderException("Replay address is null");
        }
        ByteBuf in = Unpooled.buffer();
        DatagramPacket data = msg.packet();
        Socks5CommandRequest request = Socks5.toCommandRequest(Socks5CommandType.CONNECT, data.recipient());
        cipher.encode(new RequestHeader(Network.UDP, null, request), data.content(), in);
        out.add(new DatagramPacket(in, proxy, data.sender()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        List<Object> list = new ArrayList<>(2);
        cipher.decode(new RequestHeader(Network.UDP, null, null), msg.content(), list);
        out.add(new DatagramPacket((ByteBuf) list.get(1), (InetSocketAddress) list.get(0), msg.sender()));
    }
}