package com.urbanspork.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.cipher.ShadowsocksCipher;
import com.urbanspork.cipher.ShadowsocksKey;
import com.urbanspork.common.Attributes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RemoteReceiveHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final Logger logger = LoggerFactory.getLogger(RemoteReceiveHandler.class);

    private final Channel localChannel;
    private ByteBuf buff;

    public RemoteReceiveHandler(Channel localChannel, ByteBuf buff) {
        this.localChannel = localChannel;
        this.buff = buff;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(buff);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        release();
        logger.info("Channel {} inactive", ctx.channel());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ShadowsocksCipher cipher = localChannel.attr(Attributes.CIPHER).get();
        ShadowsocksKey key = localChannel.attr(Attributes.KEY).get();
        localChannel.writeAndFlush(Unpooled.wrappedBuffer(cipher.encrypt(ByteBufUtil.getBytes(msg), key)));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        release();
        logger.error("Channel " + ctx.channel() + " error, cause: ", cause);
    }

    private void release() {
        if (localChannel != null) {
            localChannel.close();
        }
        if (buff != null) {
            buff = null;
        }
    }

}
