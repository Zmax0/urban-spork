package com.urbanspork.common.protocol.vmess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

class IDTest {
    @Test
    void testNewID() {
        byte[] id = ID.newID("b831381d-6324-4d53-ad4f-8cda48b30811");
        Assertions.assertEquals("tQ2RasDOwGeYGvjl84p1jw==", Base64.getEncoder().encodeToString(id));
    }

    @Test
    void testNextID() {
        byte[] id = ID.nextID(ID.newID("b831381d-6324-4d53-ad4f-8cda48b30811"));
        Assertions.assertEquals("M042vDoMqPkmEB1F6Gwujg==", Base64.getEncoder().encodeToString(id));
    }

    @Test
    void testNewAlterIDs() {
        int count = 64;
        byte[][] alterIDs = ID.newAlterIDs(ID.newID("b831381d-6324-4d53-ad4f-8cda48b30811"), count);
        Assertions.assertEquals("bj/9x1+roVhMqHxY6MS6yg==", Base64.getEncoder().encodeToString(alterIDs[count - 1]));
    }
}
