package com.urbanspork.common.protocol.dns;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.dns.DnsResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DnsResponseDecoderTest {
    @Test
    void notResponse() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(1);
        buffer.writeShort(0);
        Assertions.assertThrows(CorruptedFrameException.class, () -> DnsResponseDecoder.decode(buffer));
    }

    @Test
    void release() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(1);
        buffer.writeShort(35555);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> DnsResponseDecoder.decode(buffer));
    }

    @Test
    void decodeTruncatedAnswer() throws Exception {
        // answer count index[6,7]
        // actual is 4
        // now is 5
        byte[] bytes = new byte[]{-58, -27, -127, -128, 0, 1, 0, 5, 0, 0, 0, 0, 3, 119, 119, 119, 7, 101, 120, 97, 109, 112, 108, 101, 3, 99, 111, 109, 0, 0, 1, 0, 1, -64, 12, 0, 5, 0, 1, 0, 0, 0, 33, 0, 34, 3, 119, 119, 119, 7, 101, 120, 97, 109, 112, 108, 101, 6, 99, 111, 109, 45, 118, 52, 9, 101, 100, 103, 101, 115, 117, 105, 116, 101, 3, 110, 101, 116, 0, -64, 45, 0, 5, 0, 1, 0, 0, 79, -16, 0, 20, 5, 97, 49, 52, 50, 50, 4, 100, 115, 99, 114, 6, 97, 107, 97, 109, 97, 105, -64, 74, -64, 91, 0, 1, 0, 1, 0, 0, 0, 20, 0, 4, 23, -47, 46, 91, -64, 91, 0, 1, 0, 1, 0, 0, 0, 20, 0, 4, 23, -47, 46, 84};
        ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
        DnsResponse dnsResponse = DnsResponseDecoder.decode(buffer);
        Assertions.assertNotNull(dnsResponse);
    }

    @Test
    void decodeTruncatedAuthority() throws Exception {
        // answer count index[8,9]
        // actual is 0
        // now is 1
        byte[] bytes = new byte[]{-58, -27, -127, -128, 0, 1, 0, 4, 0, 1, 0, 0, 3, 119, 119, 119, 7, 101, 120, 97, 109, 112, 108, 101, 3, 99, 111, 109, 0, 0, 1, 0, 1, -64, 12, 0, 5, 0, 1, 0, 0, 0, 33, 0, 34, 3, 119, 119, 119, 7, 101, 120, 97, 109, 112, 108, 101, 6, 99, 111, 109, 45, 118, 52, 9, 101, 100, 103, 101, 115, 117, 105, 116, 101, 3, 110, 101, 116, 0, -64, 45, 0, 5, 0, 1, 0, 0, 79, -16, 0, 20, 5, 97, 49, 52, 50, 50, 4, 100, 115, 99, 114, 6, 97, 107, 97, 109, 97, 105, -64, 74, -64, 91, 0, 1, 0, 1, 0, 0, 0, 20, 0, 4, 23, -47, 46, 91, -64, 91, 0, 1, 0, 1, 0, 0, 0, 20, 0, 4, 23, -47, 46, 84};
        ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
        DnsResponse dnsResponse = DnsResponseDecoder.decode(buffer);
        Assertions.assertNotNull(dnsResponse);
    }
}
