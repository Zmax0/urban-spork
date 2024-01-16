package com.urbanspork.test.template;

import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;

@Tag("integration")
class TestUtilTestCase {
    @RepeatedTest(1000)
    void testFreePorts() {
        int[] ports = TestUtil.freePorts(2);
        Assertions.assertNotEquals(ports[0], ports[1]);
    }
}
