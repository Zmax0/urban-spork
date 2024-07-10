package com.urbanspork.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.codec.socksx.SocksVersion;

import java.util.List;

class ClientProxyUnificationHandler extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        byte versionVal = in.getByte(in.readerIndex());
        SocksVersion version = SocksVersion.valueOf(versionVal);
        ChannelPipeline p = ctx.pipeline();
        if (version == SocksVersion.UNKNOWN) {
            p.addLast(HttpPortUnificationHandler.INSTANCE);
        } else {
            p.addLast(new SocksPortUnificationServerHandler(), ClientSocksMessageHandler.INSTANCE);
        }
        p.remove(this);
    }
}
