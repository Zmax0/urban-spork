package com.urbanspork.common.config;

import java.util.Arrays;

public class ClientConfigTestCase {
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
