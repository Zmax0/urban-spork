package com.urbanspork.test.client;

import java.net.InetAddress;

abstract class TestClientTemplate {
    protected final String proxyHost;
    protected final int proxyPort;
    protected final String dstAddress;

    protected TestClientTemplate() {
        String localhost = InetAddress.getLoopbackAddress().getHostName();
        this.proxyHost = System.getProperty("proxyHost", localhost);
        this.proxyPort = Integer.parseInt(System.getProperty("proxyPort", "1089"));
        this.dstAddress = System.getProperty("dstHost", localhost);
    }
}
