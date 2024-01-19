package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

@DisplayName("Shadowsocks - Control")
class ControlTestCase {
    @Test
    void testIncreasePacketId() {
        Control control = new Control(null, 1, 1, Long.MAX_VALUE, null);
        control.increasePacketId(1);
        Assertions.assertEquals(0, control.getPacketId());
        Assertions.assertNotEquals(1, control.getClientSessionId());
    }

    @Test
    void testGetterAndSetter() {
        byte[] salt = Dice.rollBytes(32);
        long id = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        Control control = new Control(salt, 0, 0, 0, null);
        TestUtil.testGetterAndSetter(id, control, Control::getClientSessionId, Control::setClientSessionId);
        TestUtil.testGetterAndSetter(id, control, Control::getServerSessionId, Control::setServerSessionId);
        TestUtil.testGetterAndSetter(id, control, Control::getPacketId, Control::setPacketId);
        Assertions.assertArrayEquals(salt, control.salt());
    }
}
