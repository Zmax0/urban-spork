package com.urbanspork.common.protocol.shadowsocks.aead;

import com.urbanspork.common.protocol.shadowsocks.Session;
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
        Session session = new Session(0, 0, 0);
        long id = ThreadLocalRandom.current().nextLong();
        testGetterAndSetter(id, session, Session::getPacketId, Session::setPacketId);
        testGetterAndSetter(id, session, Session::getClientSessionId, Session::setClientSessionId);
        testGetterAndSetter(id, session, Session::getServerSessionId, Session::setServerSessionId);
    }

    private static <T, U, R> void testGetterAndSetter(U u, T t, Function<T, R> getter, BiConsumer<T, U> setter) {
        Assertions.assertNotEquals(u, getter.apply(t));
        setter.accept(t, u);
        Assertions.assertEquals(u, getter.apply(t));
    }
}
