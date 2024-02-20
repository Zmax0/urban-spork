package com.urbanspork.common.protocol.vmess;

import com.urbanspork.common.protocol.InetSocketAddressProvider;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.EncoderException;
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
    void testAddressing(InetSocketAddress in) {
        ByteBuf buffer = Unpooled.directBuffer();
        Address.writeAddressPort(buffer, in);
        InetSocketAddress out = Address.readAddressPort(buffer);
        Assertions.assertEquals(in.getHostString(), out.getHostString());
        Assertions.assertEquals(in.getPort(), out.getPort());
    }

    @Test
    void testWriteUnknown() {
        ByteBuf buf = Unpooled.buffer();
        Assertions.assertThrows(EncoderException.class, () -> Address.writeAddressPort(buf, null));
    }

    @Test
    void testReadUnknown() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(TestDice.rollPort());
        buf.writeByte(-1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> Address.readAddressPort(buf));
    }
}