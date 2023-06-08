package com.urbanspork.common.config;

import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class ClientConfigTestCase {

    @Test
    void testGetCurrent() {
        ClientConfig config = testConfig(TestDice.randomPort(), TestDice.randomPort());
        config.setServers(null);
        Assertions.assertNull(config.getCurrent());
    }

    public static ClientConfig testConfig(int... ports) {
        if (ports.length < 2) {
            throw new IllegalArgumentException("At least 2 ports");
        }
        ClientConfig config = new ClientConfig();
        config.setPort(ports[0]);
        config.setIndex(0);
        int[] serverPorts = Arrays.copyOfRange(ports, 1, ports.length);
        config.setServers(ServerConfigTestCase.testConfig(serverPorts));
        return config;
    }
}
