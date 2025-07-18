package com.urbanspork.common.protocol.vmess.header;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class RequestCommandTest {
    @Test
    void testEquals() {
        RequestCommand tcp = RequestCommand.TCP;
        Assertions.assertNotEquals(new Object(), tcp);
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