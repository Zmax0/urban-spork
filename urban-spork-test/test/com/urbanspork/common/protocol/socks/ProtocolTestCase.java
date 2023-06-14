package com.urbanspork.common.protocol.socks;

import com.urbanspork.common.protocol.InetSocketAddressProvider;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.InetSocketAddress;

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
    void testAddressing(InetSocketAddress address) throws Exception {
        ByteBuf out = Unpooled.directBuffer();
        Address.encode(Socks5CommandType.CONNECT, address, out);
        InetSocketAddress actual = Address.decode(out);
        Assertions.assertEquals(address, actual);
        Assertions.assertEquals(address, actual);
    }
}
