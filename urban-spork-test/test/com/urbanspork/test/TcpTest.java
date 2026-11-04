package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.test.template.TcpTestTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

class TcpTest extends TcpTestTemplate {
    @Test
    void testHttpBadRequest() throws ExecutionException, InterruptedException {
        ClientConfig config = testConfig();
        try (Client.Instance client = launchClient(config)) {
            InetSocketAddress proxyAddress = client.udp().localAddress();
            Assertions.assertThrows(ExecutionException.class, () -> checkHttpSendBytes(proxyAddress, proxyAddress));
        }
    }

    @Test
    void testConnectServerFailed() throws ExecutionException, InterruptedException {
        ClientConfig config = ClientConfigTest.testConfig(CLIENT_PORT, TestDice.rollPort());
        try (Client.Instance client = launchClient(config)) {
            InetSocketAddress clientAddress = client.tcp().localAddress();
            Assertions.assertThrows(ExecutionException.class, () -> socksHandshakeAndSendBytes(clientAddress));
            Assertions.assertThrows(ExecutionException.class, () -> checkHttpsHandshakeAndSendBytes(clientAddress));
            Assertions.assertThrows(ExecutionException.class, () -> checkHttpSendBytes(clientAddress));
        }
    }
}
