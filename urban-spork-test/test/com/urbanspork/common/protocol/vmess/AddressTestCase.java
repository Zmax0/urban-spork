package com.urbanspork.common.protocol.vmess;

import com.urbanspork.common.protocol.InetSocketAddressProvider;
import com.urbanspork.common.protocol.socks.Socks5;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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

import java.net.InetSocketAddress;

@DisplayName("VMess - Address")
class AddressTestCase {
    @ParameterizedTest
    @ArgumentsSource(InetSocketAddressProvider.class)
    void testAddressing(InetSocketAddress address) {
        ByteBuf out = Unpooled.directBuffer();
        Address.writeAddressPort(out, Socks5.toCommandRequest(Socks5CommandType.CONNECT, address));
        Socks5CommandRequest request = Address.readAddressPort(out);
        Assertions.assertEquals(address.getHostString(), request.dstAddr());
        Assertions.assertEquals(address.getPort(), request.dstPort());
    }

    @Test
    void testWriteUnknown() {
        ByteBuf buf = Unpooled.buffer();
        DefaultSocks5CommandRequest request1 = new DefaultSocks5CommandRequest(
            Socks5CommandType.CONNECT, Socks5AddressType.valueOf((byte) -1), "localhost", TestDice.rollPort());
        Assertions.assertThrows(EncoderException.class, () -> Address.writeAddressPort(buf, request1));
        DefaultSocks5CommandRequest request2 = new DefaultSocks5CommandRequest(
            Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, "", TestDice.rollPort());
        Assertions.assertThrows(EncoderException.class, () -> Address.writeAddressPort(buf, request2));
    }
}