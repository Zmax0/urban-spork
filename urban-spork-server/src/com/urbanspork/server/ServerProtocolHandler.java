package com.urbanspork.server;

import com.urbanspork.cipher.ShadowsocksCipher;
import com.urbanspork.common.Attributes;
import com.urbanspork.key.ShadowsocksKey;
import com.urbanspork.protocol.ShadowsocksProtocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerProtocolHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
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
            ShadowsocksCipher cipher = channel.attr(Attributes.CIPHER).get();
            ShadowsocksKey key = channel.attr(Attributes.KEY).get();
            byte[] decrypt = cipher.decrypt(ByteBufUtil.getBytes(buff), key);
            ByteBuf decryptedBuff = Unpooled.buffer();
            decryptedBuff.writeBytes(decrypt);
            if (decryptedBuff.readableBytes() >= 2) {
                channel.attr(Attributes.REMOTE_ADDRESS).set(ShadowsocksProtocol.decodeAddress(decryptedBuff));
                channel.pipeline().addLast(new ServerProcessor(ctx, decryptedBuff)).remove(this);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
