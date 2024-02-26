package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Shadowsocks - TCP Context")
class ContextTestCase {
    @Test
    void testEqualsAndHashcode() {
        TestUtil.testEqualsAndHashcode(new Context.Key(new byte[]{1, 2, 3}), new Context.Key(new byte[]{1, 2, 4}));
    }
}
