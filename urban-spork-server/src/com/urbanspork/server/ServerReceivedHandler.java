package com.urbanspork.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.cipher.ShadowsocksCipher;
import com.urbanspork.common.Attributes;
import com.urbanspork.key.ShadowsocksKey;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

public class ServerReceivedHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final Logger logger = LoggerFactory.getLogger(ServerReceivedHandler.class);

    private final Channel channel;
    private ByteBuf cache;

    public ServerReceivedHandler(Channel channel, ByteBuf cache) {
        this.channel = channel;
        this.cache = cache;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(cache);
        ReferenceCountUtil.release(cache);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        release();
        logger.debug("Channel {} inactive", ctx.channel());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ShadowsocksCipher cipher = channel.attr(Attributes.CIPHER).get();
        ShadowsocksKey key = channel.attr(Attributes.KEY).get();
        channel.writeAndFlush(Unpooled.wrappedBuffer(cipher.encrypt(ByteBufUtil.getBytes(msg), key)));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        release();
        logger.error("Channel " + ctx.channel() + " error, cause: ", cause);
    }

    private void release() {
        if (channel != null) {
            channel.close();
        }
        if (cache != null) {
            cache = null;
        }
    }

}
