package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.protocol.shadowsocks.Session;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Shadowsocks - Session")
class SessionTestCase {
    @Test
    void testGetterAndSetter() {
        Session session = new Session(TestDice.rollCipher());
        byte[] requestSalt = Dice.rollBytes(32);
        TestUtil.testGetterAndSetter(requestSalt, session, Session::getRequestSalt, Session::setRequestSalt);
        Assertions.assertArrayEquals(requestSalt, session.getRequestSalt());
    }
}
