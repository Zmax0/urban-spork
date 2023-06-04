package com.urbanspork.common.protocol.vmess;

import com.urbanspork.common.protocol.InetSocketAddressProvider;
import com.urbanspork.common.protocol.socks.Socks5;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.InetSocketAddress;

@DisplayName("VMess - Address")
class AddressTestCase {
    @ParameterizedTest
    @ArgumentsSource(InetSocketAddressProvider.class)
    void testAddressing(InetSocketAddress address) {
        ByteBuf out = Unpooled.directBuffer();
        Address.writeAddressPort(out, Socks5.toCommandRequest(Socks5CommandType.CONNECT, address));
        Assertions.assertEquals(address, Address.readAddressPort(out));
    }
}