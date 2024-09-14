package com.urbanspork.common.protocol.shadowsocks;

import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

class ControlTest {
    @Test
    void testIncreasePacketId() {
        Control control = new Control(1, 1, Long.MAX_VALUE);
        control.increasePacketId(1);
        Assertions.assertEquals(0, control.getPacketId());
        Assertions.assertNotEquals(1, control.getClientSessionId());
    }

    @Test
    void testGetterAndSetter() {
        long id = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        Control control = new Control(0, 0, 0);
        TestUtil.testGetterAndSetter(id, control, Control::getClientSessionId, Control::setClientSessionId);
        TestUtil.testGetterAndSetter(id, control, Control::getServerSessionId, Control::setServerSessionId);
        TestUtil.testGetterAndSetter(id, control, Control::getPacketId, Control::setPacketId);
    }
}
