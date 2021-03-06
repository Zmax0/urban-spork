package com.urbanspork.server;

import com.urbanspork.common.channel.ChannelCloseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.protocol.ShadowsocksProtocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerProtocolHandler extends ChannelInboundHandlerAdapter implements ShadowsocksProtocol {

    private static final Logger logger = LoggerFactory.getLogger(ServerProtocolHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf _msg = (ByteBuf) msg;
            Channel channel = ctx.channel();
            if (_msg.readableBytes() >= 2) {
                channel.attr(AttributeKeys.REMOTE_ADDRESS).set(decodeAddress(_msg));
                channel.pipeline().addLast(new RemoteFrontendHandler()).remove(this);
                ctx.fireChannelActive();
                ctx.fireChannelRead(_msg);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ChannelCloseUtils.closeOnFlush(ctx.channel());
    }

}
