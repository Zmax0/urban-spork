package com.urbanspork.common.config;

import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

@DisplayName("Common - Client Config")
public class ClientConfigTestCase {
    @Test
    void testToString() {
        ClientConfig config = testConfig(TestDice.rollPort(), TestDice.rollPort());
        Assertions.assertFalse(config.toString().isEmpty());
    }

    @Test
    void testGetCurrent() {
        ClientConfig config = testConfig(TestDice.rollPort(), TestDice.rollPort());
        config.setServers(Collections.emptyList());
        Assertions.assertNull(config.getCurrent());
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
        config.setServers(ServerConfigTestCase.testConfigs(serverPorts));
        return config;
    }
}
