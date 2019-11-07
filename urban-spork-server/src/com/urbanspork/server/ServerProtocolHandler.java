package com.urbanspork.server;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.protocol.ShadowsocksProtocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerProtocolHandler extends ChannelInboundHandlerAdapter implements ShadowsocksProtocol {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // skip
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            Channel channel = ctx.channel();
            ByteBuf buff = (ByteBuf) msg;
            if (buff.readableBytes() >= 2) {
                channel.attr(AttributeKeys.REMOTE_ADDRESS).set(decodeAddress(buff));
                channel.pipeline().addLast(new RemoteConnectHandler(channel, buff)).remove(this);
            } else {
                ctx.close();
            }
        } else {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
