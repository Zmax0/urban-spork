package com.urbanspork.common.protocol.vmess.header;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class RequestCommandTestCase {
    @Test
    void testEquals() {
        RequestCommand tcp = RequestCommand.TCP;
        Assertions.assertNotEquals(tcp, new Object());
        Assertions.assertEquals(tcp, new RequestCommand(tcp.value()));
    }

    @Test
    void testEqualsAndHashcode() {
        RequestCommand tcp = RequestCommand.TCP;
        Set<RequestCommand> set = new HashSet<>();
        set.add(tcp);
        RequestCommand another = new RequestCommand(tcp.value());
        set.add(another);
        Assertions.assertEquals(1, set.size());
    }
}