package com.urbanspork.common.codec.shadowsocks.udp;

import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.List;

interface AeadCipherCodec {

    void encode(Context context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException;

    void decode(Context context, ByteBuf in, List<Object> out) throws InvalidCipherTextException;
}
