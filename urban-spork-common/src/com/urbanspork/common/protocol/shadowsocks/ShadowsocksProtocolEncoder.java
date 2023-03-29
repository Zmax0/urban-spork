package com.urbanspork.common.protocol.shadowsocks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class ShadowsocksProtocolEncoder extends MessageToByteEncoder<ByteBuf> implements ShadowsocksProtocol {

    private final Socks5CommandRequest request;

    private volatile boolean encoded = false;

    public ShadowsocksProtocolEncoder(Socks5CommandRequest request) {
        this.request = request;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        if (!encoded) {
            encoded = true;
            encodeAddress(request, out);
        }
        out.writeBytes(msg);
    }
}