package com.urbanspork.common.protocol.socks;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.socksx.v5.Socks5AddressDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

public interface Socks5Addressing {

    static Socks5AddressType map(InetAddress address) {
        if (address instanceof Inet4Address) {
            return Socks5AddressType.IPv4;
        } else if (address instanceof Inet6Address) {
            return Socks5AddressType.IPv6;
        } else {
            return Socks5AddressType.DOMAIN;
        }
    }

    static void encode(InetSocketAddress address, ByteBuf out) throws Exception {
        InetAddress inetAddress = address.getAddress();
        Socks5AddressType addressType = map(inetAddress);
        out.writeByte(addressType.byteValue());
        Socks5AddressEncoder.DEFAULT.encodeAddress(addressType, inetAddress.getHostAddress(), out);
        out.writeShort(address.getPort());
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
        return new InetSocketAddress(hostname, in.readUnsignedShort());
    }

}
