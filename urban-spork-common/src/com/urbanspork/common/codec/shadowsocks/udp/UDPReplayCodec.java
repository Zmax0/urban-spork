package com.urbanspork.common.codec.shadowsocks.udp;

import com.urbanspork.common.codec.shadowsocks.Mode;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.shadowsocks.aead2022.Control;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageCodec;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class UDPReplayCodec extends MessageToMessageCodec<DatagramPacket, TernaryDatagramPacket> {

    private final AeadCipherCodec cipher;
    private final Mode mode;
    private final Control control;
    private final ServerUserManager userManager;

    public UDPReplayCodec(ServerConfig config, Mode mode) {
        this.cipher = AeadCipherCodecs.get(config);
        this.mode = mode;
        this.control = new Control(config.getCipher());
        this.userManager = Mode.Server == mode ? ServerUserManager.DEFAULT : ServerUserManager.EMPTY;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, TernaryDatagramPacket msg, List<Object> out) throws Exception {
        InetSocketAddress proxy = msg.third();
        if (proxy == null) {
            throw new EncoderException("Replay address is null");
        }
        ByteBuf in = Unpooled.buffer();
        DatagramPacket data = msg.packet();
        cipher.encode(new Context(mode, control, data.recipient(), userManager), data.content(), in);
        control.increasePacketId(1);
        out.add(new DatagramPacket(in, proxy, data.sender()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        List<Object> list = new ArrayList<>(2);
        cipher.decode(new Context(mode, control, null, userManager), msg.content(), list);
        out.add(new DatagramPacket((ByteBuf) list.get(1), (InetSocketAddress) list.get(0), msg.sender()));
    }
}