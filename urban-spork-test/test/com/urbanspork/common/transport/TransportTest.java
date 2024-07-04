package com.urbanspork.common.transport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TransportTest {
    @Test
    void testToMap() {
        Assertions.assertNull(Transport.toMap(null));
        Assertions.assertEquals(0, Transport.toMap(new Transport[]{}).length);
        Transport[] regroup = Transport.toMap(new Transport[]{Transport.WS, Transport.WS, Transport.TCP, Transport.TCP, Transport.WS, Transport.UDP, Transport.WS, Transport.TCP, Transport.UDP});
        Assertions.assertEquals(2, regroup.length);
        Assertions.assertEquals(Transport.UDP, regroup[0]);
        Assertions.assertEquals(Transport.WS, regroup[1]);
        regroup = Transport.toMap(new Transport[]{Transport.TCP, Transport.WS});
        Assertions.assertEquals(2, regroup.length);
        Assertions.assertNull(regroup[0]);
        Assertions.assertEquals(Transport.WS, regroup[1]);
        regroup = Transport.toMap(new Transport[]{Transport.TCP});
        Assertions.assertEquals(2, regroup.length);
        Assertions.assertNull(regroup[0]);
        Assertions.assertEquals(Transport.TCP, regroup[1]);
        regroup = Transport.toMap(new Transport[]{Transport.UDP});
        Assertions.assertEquals(1, regroup.length);
        Assertions.assertEquals(Transport.UDP, regroup[0]);
    }

    @Test
    void testReverseToMap() {
        Assertions.assertEquals(0, Transport.reverseToMap(new Transport[]{}).length);
        Transport[] transports = Transport.reverseToMap(new Transport[]{null, null, Transport.UDP, null, null, Transport.WS});
        Assertions.assertEquals(2, transports.length);
        Assertions.assertEquals(Transport.UDP, transports[0]);
        Assertions.assertEquals(Transport.WS, transports[1]);
    }
}
