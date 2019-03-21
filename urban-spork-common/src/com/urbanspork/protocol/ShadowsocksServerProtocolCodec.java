package com.urbanspork.protocol;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ShadowsocksServerProtocolCodec extends ShadowsocksProtocolCodec {

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        out.add(msg.retain());
    }

}
