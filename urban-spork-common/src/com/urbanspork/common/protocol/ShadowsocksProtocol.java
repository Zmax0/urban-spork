package com.urbanspork.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.socksx.v5.Socks5AddressDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import java.net.InetSocketAddress;

public interface ShadowsocksProtocol {

    default void encodeAddress(Socks5CommandRequest msg, ByteBuf out) throws Exception {
        Socks5AddressType addressType = msg.dstAddrType();
        out.writeByte(addressType.byteValue());
        Socks5AddressEncoder.DEFAULT.encodeAddress(addressType, msg.dstAddr(), out);
        out.writeShort(msg.dstPort());
    }

    default InetSocketAddress decodeAddress(Socks5AddressType addressType, ByteBuf in) throws Exception {
        return new InetSocketAddress(Socks5AddressDecoder.DEFAULT.decodeAddress(addressType, in), in.readUnsignedShort());
    }

}
