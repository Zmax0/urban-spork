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
        // fired by channelRead
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf _msg = (ByteBuf) msg;
            Channel channel = ctx.channel();
            if (_msg.readableBytes() >= 2) {
                channel.attr(AttributeKeys.REMOTE_ADDRESS).set(decodeAddress(_msg));
                channel.pipeline()
                    .addLast(new RemoteConnectHandler())
                    .remove(this);
                ctx.fireChannelActive();
                ctx.fireChannelRead(_msg);
            } else {
                ctx.fireExceptionCaught(new IllegalStateException("Can not decode remote address"));
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

}
