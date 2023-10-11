package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.shadowsocks.RequestHeader;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import java.util.List;

public class TCPReplayCodec extends ByteToMessageCodec<ByteBuf> {

    private final RequestHeader header;
    private final AEADCipherCodec cipher;

    public TCPReplayCodec(StreamType streamType, String password, SupportedCipher cipher) {
        this(streamType, null, password, cipher);
    }

    public TCPReplayCodec(StreamType streamType, Socks5CommandRequest request, String password, SupportedCipher cipher) {
        this.header = new RequestHeader(Network.TCP, streamType, request);
        this.cipher = AEADCipherCodecs.get(password, cipher);
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        cipher.encode(header, msg, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        cipher.decode(header, in, out);
    }
}
