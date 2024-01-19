package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Shadowsocks - Session")
class SessionTestCase {
    @Test
    void testGetterAndSetter() {
        byte[] salt = Dice.rollBytes(32);
        byte[] requestSalt = Dice.rollBytes(32);
        Session session = new Session(salt, null);
        TestUtil.testGetterAndSetter(requestSalt, session, Session::getRequestSalt, Session::setRequestSalt);
        Assertions.assertArrayEquals(salt, session.salt());
        Assertions.assertArrayEquals(requestSalt, session.getRequestSalt());
    }
}
