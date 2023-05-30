package com.urbanspork.common.protocol.socks;

import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.InetSocketAddress;
import java.util.stream.Stream;

class Socks5ProtocolTestCase {

    @ParameterizedTest
    @ArgumentsSource(InetSocketAddressProvider.class)
    void testToCommandRequest(InetSocketAddress address) {
        Socks5CommandRequest request = Socks5.toCommandRequest(Socks5CommandType.CONNECT, address);
        Assertions.assertEquals(address, new InetSocketAddress(request.dstAddr(), request.dstPort()));
    }

    @ParameterizedTest
    @ArgumentsSource(InetSocketAddressProvider.class)
    void testAddressing(InetSocketAddress address) throws Exception {
        ByteBuf out = Unpooled.directBuffer();
        Socks5Addressing.encode(Socks5CommandType.CONNECT, address, out);
        Assertions.assertEquals(address, Socks5Addressing.decode(out));
    }

    static class InetSocketAddressProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                new InetSocketAddress("www.w3.org", 80),
                InetSocketAddress.createUnresolved("www.urban-spork.com", 443),
                new InetSocketAddress(0),
                new InetSocketAddress("192.168.89.9", TestDice.randomPort()),
                new InetSocketAddress("ABCD:EF01:2345:6789:ABCD:EF01:2345:6789", TestDice.randomPort())
            ).map(Arguments::of);
        }
    }
}
