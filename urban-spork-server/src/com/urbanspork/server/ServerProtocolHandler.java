package com.urbanspork.server;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.channel.ChannelCloseUtils;
import com.urbanspork.common.protocol.ShadowsocksProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerProtocolHandler extends ChannelInboundHandlerAdapter implements ShadowsocksProtocol {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf byteBuf) {
            Channel channel = ctx.channel();
            channel.attr(AttributeKeys.REMOTE_ADDRESS).set(decodeAddress(byteBuf));
            channel.pipeline().addLast(new RemoteFrontendHandler()).remove(this);
            ctx.fireChannelActive();
            ctx.fireChannelRead(byteBuf);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ChannelCloseUtils.closeOnFlush(ctx.channel());
    }

}
