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
import io.netty.util.ReferenceCountUtil;

public class ServerProtocolHandler extends ChannelInboundHandlerAdapter {

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
            try {
                Channel channel = ctx.channel();
                ShadowsocksCipher cipher = channel.attr(Attributes.CIPHER).get();
                ShadowsocksKey key = channel.attr(Attributes.KEY).get();
                byte[] decrypt = cipher.decrypt(ByteBufUtil.getBytes((ByteBuf) msg), key);
                ByteBuf decryptedBuff = Unpooled.buffer();
                decryptedBuff.writeBytes(decrypt);
                if (decryptedBuff.readableBytes() >= 2) {
                    channel.attr(Attributes.REMOTE_ADDRESS).set(ShadowsocksProtocol.decodeAddress(decryptedBuff));
                    channel.pipeline().addLast(new RemoteConnectHandler(channel, decryptedBuff)).remove(this);
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
