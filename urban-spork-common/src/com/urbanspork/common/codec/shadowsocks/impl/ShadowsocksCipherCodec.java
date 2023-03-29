package com.urbanspork.common.codec.shadowsocks.impl;

import com.urbanspork.common.codec.aead.AEADCipherCodec;
import com.urbanspork.common.codec.shadowsocks.base.ShadowsocksAEADCipherCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static java.lang.System.arraycopy;

abstract class ShadowsocksCipherCodec extends ByteToMessageCodec<ByteBuf> {

    private final ShadowsocksAEADCipherCodec encoder;
    private final ShadowsocksAEADCipherCodec decoder;

    protected ShadowsocksCipherCodec(byte[] password, int saltSize) {
        byte[] key = generateKey(password, saltSize);
        AEADCipherCodec codec = codec();
        encoder = new ShadowsocksAEADCipherCodec(codec, key, saltSize);
        decoder = new ShadowsocksAEADCipherCodec(codec, key, saltSize);
    }

    protected abstract AEADCipherCodec codec();

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        encoder.encode(msg, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        decoder.decrypt(msg, out);
    }

    private byte[] generateKey(byte[] password, int size) {
        byte[] passwordDigest = null;
        byte[] container = null;
        int index = 0;
        byte[] encoded = new byte[size];
        MessageDigest MD5;
        try {
            MD5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e);
        }
        while (index < size) {
            if (index == 0) {
                passwordDigest = MD5.digest(password);
                container = new byte[password.length + passwordDigest.length];
            } else {
                arraycopy(passwordDigest, 0, container, 0, passwordDigest.length);
                arraycopy(password, 0, container, passwordDigest.length, password.length);
                passwordDigest = MD5.digest(container);
            }
            arraycopy(passwordDigest, 0, encoded, index, Math.min(size - index, passwordDigest.length));
            index += passwordDigest.length;
        }
        return encoded;
    }

}
