package com.urbanspork.common.transport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TransportTest {
    @Test
    void testRegroup() {
        Transport[] regroup = Transport.regroup(new Transport[]{Transport.WS, Transport.WS, Transport.TCP, Transport.TCP, Transport.WS, Transport.UDP, Transport.WS, Transport.TCP, Transport.UDP});
        Assertions.assertEquals(2, regroup.length);
        Assertions.assertEquals(Transport.UDP, regroup[0]);
        Assertions.assertEquals(Transport.WS, regroup[1]);
        regroup = Transport.regroup(new Transport[]{Transport.WS});
        Assertions.assertEquals(2, regroup.length);
        Assertions.assertNull(regroup[0]);
        Assertions.assertEquals(Transport.WS, regroup[1]);
        regroup = Transport.regroup(new Transport[]{Transport.TCP});
        Assertions.assertEquals(2, regroup.length);
        Assertions.assertNull(regroup[0]);
        Assertions.assertEquals(Transport.TCP, regroup[1]);
        regroup = Transport.regroup(new Transport[]{Transport.UDP});
        Assertions.assertEquals(1, regroup.length);
        Assertions.assertEquals(Transport.UDP, regroup[0]);
    }
}
