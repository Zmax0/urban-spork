package com.urbanspork.common.protocol.vmess;

import com.urbanspork.common.protocol.InetSocketAddressProvider;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.EncoderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;

class AddressTest {
    @ParameterizedTest
    @ArgumentsSource(InetSocketAddressProvider.class)
    void testAddressing(InetSocketAddress in) {
        ByteBuf buffer = Unpooled.buffer();
        Address.writeAddressPort(buffer, in);
        InetSocketAddress out = Address.readAddressPort(buffer);
        Assertions.assertEquals(in.getHostString(), out.getHostString());
        Assertions.assertEquals(in.getPort(), out.getPort());
        buffer.release();
    }

    @Test
    void testWriteUnknown() {
        ByteBuf buf = Unpooled.buffer();
        Assertions.assertThrows(EncoderException.class, () -> Address.writeAddressPort(buf, null));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 4})
    void testReadUnknown(int type) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(TestDice.rollPort());
        buf.writeByte(type);
        Assertions.assertThrows(IllegalArgumentException.class, () -> Address.readAddressPort(buf));
    }
}