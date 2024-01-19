package com.urbanspork.common.protocol.socks;

import com.urbanspork.common.protocol.InetSocketAddressProvider;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@DisplayName("Socks5 - Protocol")
class ProtocolTestCase {

    @ParameterizedTest
    @ArgumentsSource(InetSocketAddressProvider.class)
    void testToCommandRequest(InetSocketAddress address) {
        Socks5CommandRequest request = Socks5.toCommandRequest(Socks5CommandType.CONNECT, address);
        Assertions.assertEquals(address.getHostString(), request.dstAddr());
        Assertions.assertEquals(address.getPort(), request.dstPort());
    }

    @ParameterizedTest
    @ArgumentsSource(InetSocketAddressProvider.class)
    void testAddressing(InetSocketAddress address) {
        ByteBuf out = Unpooled.buffer();
        Address.encode(address, out);
        InetSocketAddress actual = Address.decode(out);
        Assertions.assertEquals(address, actual);
    }

    @Test
    void testGetAddressLength() throws UnknownHostException {
        Socks5CommandRequest r1 = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.IPv4, "192.168.89.9", 80);
        Socks5CommandRequest r2 = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.IPv6, "abcd:ef01:2345:6789:abcd:ef01:2345:6789", 443);
        Socks5CommandRequest r3 = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, "www.example.com", 443);
        Socks5CommandRequest r4 = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.valueOf((byte) -1), "www.example.com", 443);
        Assertions.assertEquals(7, Address.getLength(r1));
        Assertions.assertEquals(19, Address.getLength(r2));
        Assertions.assertEquals(19, Address.getLength(r3));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> Address.getLength(r4));
        InetSocketAddress addr1 = new InetSocketAddress(InetAddress.getByName("192.168.89.9"), 80);
        InetSocketAddress addr2 = new InetSocketAddress(InetAddress.getByName("abcd:ef01:2345:6789:abcd:ef01:2345:6789"), 443);
        InetSocketAddress addr3 = InetSocketAddress.createUnresolved("www.example.com", 443);
        Assertions.assertEquals(7, Address.getLength(addr1));
        Assertions.assertEquals(19, Address.getLength(addr2));
        Assertions.assertEquals(19, Address.getLength(addr3));
    }

    @Test
    void testFailedAddressing() {
        DefaultSocks5CommandRequest r1 = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.valueOf((byte) -1), "192.168.89.9", 80);
        DefaultSocks5CommandRequest r2 = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.IPv4, "192.168.89.9", 80);
        ByteBuf out = Unpooled.buffer();
        Assertions.assertThrows(EncoderException.class, () -> Address.encode(r1, out));
        Address.encode(r2, out);
        out.setByte(0, -1);
        Assertions.assertThrows(DecoderException.class, () -> Address.decode(out));
    }
}
