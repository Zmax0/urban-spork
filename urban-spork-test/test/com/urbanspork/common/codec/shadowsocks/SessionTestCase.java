package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.util.Dice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Function;

@DisplayName("Shadowsocks - Session")
class SessionTestCase {
    @Test
    void testGetterAndSetter() {
        byte[] salt = Dice.rollBytes(32);
        byte[] requestSalt = Dice.rollBytes(32);
        Network network = ThreadLocalRandom.current().nextBoolean() ? Network.UDP : Network.TCP;
        Session session = new Session(network, 0, 0, 0, salt, null);
        long id = ThreadLocalRandom.current().nextLong();
        testGetterAndSetter(id, session, Session::getPacketId, Session::setPacketId);
        testGetterAndSetter(id, session, Session::getClientSessionId, Session::setClientSessionId);
        testGetterAndSetter(id, session, Session::getServerSessionId, Session::setServerSessionId);
        testGetterAndSetter(requestSalt, session, Session::getRequestSalt, Session::setRequestSalt);
        Assertions.assertArrayEquals(salt, session.salt());
        Assertions.assertArrayEquals(requestSalt, session.getRequestSalt());
    }

    @Test
    void testIncreasePacketId() {
        Control control = new Control(1, 1, Long.MAX_VALUE, null);
        control.increasePacketId(1);
        Assertions.assertEquals(0, control.getPacketId());
        Assertions.assertNotEquals(1, control.getClientSessionId());
    }

    private static <T, U, R> void testGetterAndSetter(U u, T t, Function<T, R> getter, BiConsumer<T, U> setter) {
        Assertions.assertNotEquals(u, getter.apply(t));
        setter.accept(t, u);
        Assertions.assertEquals(u, getter.apply(t));
    }
}
