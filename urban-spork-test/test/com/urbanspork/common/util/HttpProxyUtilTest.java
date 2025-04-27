package com.urbanspork.common.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpProxyUtilTest {
    @ParameterizedTest
    @ValueSource(strings = {"""
        CONNECT .example.com:443 HTTP/1.1
        Proxy-Connection: keep-alive
        """, """
        POST http://.example.com:8080/?a=b&c=d HTTP/1.1
        Connection: keep-alive
        """, """
        GET http://.example.com/?a=b&c=d HTTP/1.1
        Connection: keep-alive
        """, """
        PUT http://[::1] HTTP/1.1
        Connection: keep-alive
        """, """
        OPTIONS http://[0:0:0:0:0:0:0:1]:8080 HTTP/1.1
        Connection: keep-alive
        """})
    void testParseOption(String msg) {
        HttpProxyUtil.Option option = HttpProxyUtil.parseOption(Unpooled.wrappedBuffer(msg.getBytes()));
        Assertions.assertNotNull(option.method());
        Assertions.assertNotNull(option.address());
    }

    @Test
    void testParseIllegalInitialLine() {
        String str = "GET " + (char) 10 + "http://.example.com HTTP/1.1\r\nConnection: keep-alive\r\n\r\n";
        ByteBuf msg = Unpooled.wrappedBuffer(str.getBytes());
        Assertions.assertThrows(IllegalArgumentException.class, () -> HttpProxyUtil.parseOption(msg));
        ByteBuf msg2 = Unpooled.wrappedBuffer("GET".getBytes());
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> HttpProxyUtil.parseOption(msg2));
    }
}
