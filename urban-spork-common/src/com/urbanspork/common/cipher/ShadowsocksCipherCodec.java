package com.urbanspork.common.cipher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

public class ShadowsocksCipherCodec extends ByteToMessageCodec<ByteBuf> {

    private final ShadowsocksCipher cipher;

    private final ShadowsocksKey key;

    public ShadowsocksCipherCodec(ShadowsocksCipher cipher, ShadowsocksKey key) {
        this.cipher = cipher;
        this.key = key;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        out.writeBytes(cipher.encrypt(msg, key));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        List<ByteBuf> decrypt = cipher.decrypt(msg, key);
        if (!decrypt.isEmpty()) {
            out.addAll(decrypt);
        }
    }

}
