package com.urbanspork.common.protocol;

import com.urbanspork.common.channel.AttributeKeys;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import java.net.UnknownHostException;

public class ShadowsocksProtocolEncoder extends MessageToByteEncoder<ByteBuf> implements ShadowsocksProtocol {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws UnknownHostException {
        Socks5CommandRequest request = ctx.channel().attr(AttributeKeys.REQUEST).getAndSet(null);
        if (request != null) {
            out.writeBytes(encodeRequest(request));
        }
        out.writeBytes(msg);
    }
}
