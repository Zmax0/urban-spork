package com.urbanspork.test.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;

import java.net.InetAddress;

abstract class TestClientTemplate {
    protected final int proxyPort;
    protected final String hostname;

    protected TestClientTemplate() {
        int proxyPort = 1089;
        String hostname;
        try {
            ClientConfig config = ConfigHandler.DEFAULT.read();
            proxyPort = config.getPort();
            hostname = config.getCurrent().getHost();
        } catch (Exception ignore) {
            hostname = InetAddress.getLoopbackAddress().getHostName();
        }
        this.proxyPort = proxyPort;
        this.hostname = hostname;
    }
}
