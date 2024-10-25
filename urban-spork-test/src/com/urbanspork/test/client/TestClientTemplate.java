package com.urbanspork.test.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;

import java.net.InetAddress;

abstract class TestClientTemplate {
    protected final String proxyHost;
    protected final int proxyPort;
    protected final String hostname;

    protected TestClientTemplate() {
        int proxyPort = 1089;
        String proxyHost;
        String hostname;
        try {
            ClientConfig config = ConfigHandler.DEFAULT.read();
            proxyPort = config.getPort();
            proxyHost = config.getHost();
            hostname = config.getCurrent().getHost();
        } catch (Exception ignore) {
            String localhost = InetAddress.getLoopbackAddress().getHostName();
            proxyHost = localhost;
            hostname = localhost;
        }
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.hostname = hostname;
    }
}
