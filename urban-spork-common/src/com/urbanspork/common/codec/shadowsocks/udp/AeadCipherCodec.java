package com.urbanspork.common.codec.shadowsocks.udp;

import com.urbanspork.common.transport.udp.RelayingPacket;
import io.netty.buffer.ByteBuf;

interface AeadCipherCodec {

    void encode(Context context, ByteBuf msg, ByteBuf out) throws Exception;

    RelayingPacket<ByteBuf> decode(Context context, ByteBuf in) throws Exception;
}
