package com.urbanspork.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.codec.socksx.SocksVersion;

import java.util.List;

class ClientProxyUnificationHandler extends ByteToMessageDecoder {
    private final ClientChannelContext context;

    ClientProxyUnificationHandler(ClientChannelContext context) {
        this.context = context;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        SocksVersion version = SocksVersion.valueOf(in.getByte(in.readerIndex()));
        ChannelPipeline p = ctx.pipeline();
        if (version == SocksVersion.UNKNOWN) {
            p.addLast(new ClientHttpUnificationHandler(context));
        } else {
            p.addLast(new SocksPortUnificationServerHandler(), new ClientSocksMessageHandler(context));
        }
        p.remove(this);
    }
}
