package com.urbanspork.common.protocol.vmess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

class IDTestCase {

    @Test
    void testNewID() {
        byte[] id = ID.newID("b831381d-6324-4d53-ad4f-8cda48b30811");
        Assertions.assertEquals("tQ2RasDOwGeYGvjl84p1jw==", Base64.getEncoder().encodeToString(id));
    }
}
