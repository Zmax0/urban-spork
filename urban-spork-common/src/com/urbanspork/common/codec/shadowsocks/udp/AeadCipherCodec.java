package com.urbanspork.common.codec.shadowsocks.udp;

import com.urbanspork.common.transport.udp.RelayingPacket;
import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

interface AeadCipherCodec {

    void encode(Context context, ByteBuf msg, ByteBuf out) throws InvalidCipherTextException;

    RelayingPacket<ByteBuf> decode(Context context, ByteBuf in) throws InvalidCipherTextException;
}
