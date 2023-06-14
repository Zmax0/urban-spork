package com.urbanspork.common.protocol.socks;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.socksx.v5.*;

import java.net.InetSocketAddress;
import java.util.List;

public interface Address {

    static void encode(Socks5CommandType type, InetSocketAddress address, ByteBuf out) throws Exception {
        encode(Socks5.toCommandRequest(type, address), out);
    }

    static void encode(Socks5CommandRequest request, ByteBuf out) throws Exception {
        Socks5AddressType addressType = request.dstAddrType();
        out.writeByte(addressType.byteValue());
        Socks5AddressEncoder.DEFAULT.encodeAddress(addressType, request.dstAddr(), out);
        out.writeShort(request.dstPort());
    }

    static void decode(ByteBuf in, List<Object> out) throws Exception {
        out.add(decode(in));
    }

    static InetSocketAddress decode(ByteBuf in) throws Exception {
        Socks5AddressType addressType = Socks5AddressType.valueOf(in.readByte());
        String hostname = Socks5AddressDecoder.DEFAULT.decodeAddress(addressType, in);
        if (addressType == Socks5AddressType.DOMAIN) {
            return InetSocketAddress.createUnresolved(hostname, in.readUnsignedShort());
        } else {
            return new InetSocketAddress(hostname, in.readUnsignedShort());
        }
    }
}