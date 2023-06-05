package com.urbanspork.common.protocol.shadowsocks;

import com.urbanspork.common.protocol.socks.Address;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class ShadowsocksAddressEncoder extends MessageToByteEncoder<ByteBuf> {

    private final Socks5CommandRequest request;

    public ShadowsocksAddressEncoder(Socks5CommandRequest request) {
        this.request = request;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        ctx.pipeline().remove(this);
        Address.encode(request, out);
        out.writeBytes(msg);
    }

}
