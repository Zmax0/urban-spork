package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

@DisplayName("Shadowsocks - TCP Context")
class ContextTestCase {
    @Test
    void testEqualsAndHashcode() {
        TestUtil.testEqualsAndHashcode(new Context.Key(new byte[]{1, 2, 3}), new Context.Key(new byte[]{1, 2, 4}));
    }

    @Test
    void testToString() {
        byte[] nonce = {1, 2, 3};
        Context.Key key = new Context.Key(nonce);
        Assertions.assertEquals(Arrays.toString(nonce), key.toString());
    }
}
