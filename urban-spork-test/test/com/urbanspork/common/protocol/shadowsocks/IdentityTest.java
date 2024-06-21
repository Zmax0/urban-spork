package com.urbanspork.common.protocol.shadowsocks;

import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IdentityTest {
    @Test
    void testGetterAndSetter() {
        Identity session = new Identity(TestDice.rollCipher());
        byte[] requestSalt = Dice.rollBytes(32);
        TestUtil.testGetterAndSetter(requestSalt, session, Identity::getRequestSalt, Identity::setRequestSalt);
        Assertions.assertArrayEquals(requestSalt, session.getRequestSalt());
    }
}
