package com.urbanspork.common.cipher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

public class ShadowsocksCipherCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {

    private final ShadowsocksCipher cipher;

    private final ShadowsocksKey key;

    public ShadowsocksCipherCodec(ShadowsocksCipher cipher, ShadowsocksKey key) {
        this.cipher = cipher;
        this.key = key;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        out.add(cipher.encrypt(msg, key));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        out.add(cipher.decrypt(msg, key));
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        cipher.releaseBuffer();
    }

}
