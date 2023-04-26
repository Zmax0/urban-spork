package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.protocol.socks.Socks5Addressing;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ShadowsocksUDPReplayCodec extends MessageToMessageCodec<DatagramPacket, DatagramPacket> {

    private final ShadowsocksAEADCipherCodec cipher;

    public ShadowsocksUDPReplayCodec(ShadowsocksAEADCipherCodec cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        InetSocketAddress replayAddress = ctx.channel().attr(AttributeKeys.REPLAY_ADDRESS).get();
        if (replayAddress == null) {
            ctx.fireExceptionCaught(new IllegalStateException("Replay address is null"));
            return;
        }
        ByteBuf in = ctx.alloc().buffer();
        Socks5Addressing.encode(msg.recipient(), in);
        in.writeBytes(msg.content());
        ByteBuf content = ctx.alloc().buffer();
        cipher.encode(ctx, in, content);
        DatagramPacket encoded = new DatagramPacket(content, replayAddress, msg.sender());
        out.add(encoded);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        List<Object> list = new ArrayList<>(1);
        cipher.decode(ctx, msg.content(), list);
        ByteBuf in = (ByteBuf) list.get(0);
        InetSocketAddress recipient = Socks5Addressing.decode(in);
        DatagramPacket decoded = new DatagramPacket(in.retainedSlice(), recipient, msg.sender());
        out.add(decoded);
    }
}