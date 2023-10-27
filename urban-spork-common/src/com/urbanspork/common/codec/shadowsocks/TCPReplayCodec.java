package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.shadowsocks.RequestContext;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import java.util.List;

public class TCPReplayCodec extends ByteToMessageCodec<ByteBuf> {

    private final RequestContext context;
    private final AEADCipherCodec cipher;

    public TCPReplayCodec(StreamType streamType, CipherKind cipher, String password) {
        this(streamType, null, cipher, password);
    }

    public TCPReplayCodec(StreamType streamType, Socks5CommandRequest request, CipherKind cipher, String password) {
        this.context = new RequestContext(Network.TCP, streamType, request);
        this.cipher = AEADCipherCodecs.get(cipher, password);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        cipher.encode(context, msg, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        cipher.decode(context, in, out);
    }
}
