package com.urbanspork.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ByteStringTest {
    @Test
    void testBytesToString() {
        byte[] bytes = new byte[]{0, 9, 10, 12, 13, 14, 31, 32, 33, 127, 128 - 256, 129 - 256, 255 - 256};
        Assertions.assertEquals("\\x00\\t\\n\\x0c\\r\\x0e\\x1f !\\x7f\\x80\\x81\\xff", ByteString.valueOf(bytes));
    }
}
