package com.urbanspork.common.protocol.vmess.encoding;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VMess - Session")
public class SessionTestCase {

    @Test
    void testAnother() {
        ClientSession session = new ClientSession();
        ClientSession another = another(session);
        Assertions.assertArrayEquals(session.getRequestBodyIV(), another.getRequestBodyIV());
        Assertions.assertArrayEquals(session.getRequestBodyKey(), another.getRequestBodyKey());
        Assertions.assertArrayEquals(session.getResponseBodyIV(), another.getResponseBodyIV());
        Assertions.assertArrayEquals(session.getResponseBodyKey(), another.getResponseBodyKey());
        Assertions.assertNotEquals(session.responseHeader, another.responseHeader);
    }

    public static ClientSession another(ClientSession src) {
        byte[] bytes = new byte[33];
        System.arraycopy(src.getRequestBodyIV(), 0, bytes, 0, 16);
        System.arraycopy(src.getRequestBodyKey(), 0, bytes, 16, 16);
        bytes[32] = (byte) (src.getResponseHeader() + 1);
        return new ClientSession(bytes);
    }
}
