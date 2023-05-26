package com.urbanspork.common.protocol.socks;

import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.NetUtil;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface Socks5 {

    static Socks5CommandRequest toCommandRequest(Socks5CommandType type, InetSocketAddress address) {
        InetAddress inetAddress = address.getAddress();
        Socks5AddressType dstAddrType;
        String dstAddr;
        if (inetAddress instanceof Inet4Address v4) {
            dstAddrType = Socks5AddressType.IPv4;
            dstAddr = v4.getHostAddress();
        } else if (inetAddress instanceof Inet6Address v6) {
            dstAddrType = Socks5AddressType.IPv6;
            dstAddr = NetUtil.toAddressString(v6);
        } else {
            dstAddrType = Socks5AddressType.DOMAIN;
            dstAddr = address.getHostString();
        }
        return new DefaultSocks5CommandRequest(
            type,
            dstAddrType,
            dstAddr,
            address.getPort()
        );
    }
}
