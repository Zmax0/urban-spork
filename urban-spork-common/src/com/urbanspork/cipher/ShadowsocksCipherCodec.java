package com.urbanspork.cipher;

import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.urbanspork.common.Attributes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

public class ShadowsocksCipherCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        Channel channel = ctx.channel();
        ShadowsocksCipher cipher = Optional.of(channel.attr(Attributes.CIPHER).get()).orElseThrow(CipherNotFoundException::new);
        SecretKey key = Optional.of((channel.attr(Attributes.KEY).get())).orElseThrow(CipherNotFoundException::new);
        byte[] encrypt = cipher.encrypt(ByteBufUtil.getBytes(msg), key);
        out.add(Unpooled.buffer(encrypt.length).writeBytes(encrypt));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        Channel channel = ctx.channel();
        ShadowsocksCipher cipher = Optional.of(channel.attr(Attributes.CIPHER).get()).orElseThrow(CipherNotFoundException::new);
        SecretKey key = Optional.of((channel.attr(Attributes.KEY).get())).orElseThrow(CipherNotFoundException::new);
        byte[] decrypt = cipher.decrypt(ByteBufUtil.getBytes(msg), key);
        out.add(Unpooled.buffer(decrypt.length).writeBytes(decrypt));
    }

    private class CipherNotFoundException extends IllegalStateException {

        private static final long serialVersionUID = 20190124L;

    }

}
