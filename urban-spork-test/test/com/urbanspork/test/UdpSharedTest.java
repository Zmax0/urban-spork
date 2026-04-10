package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.server.Server;
import com.urbanspork.test.template.FutureInstance;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.UdpTestTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class UdpSharedTest extends UdpTestTemplate {
    private ServerConfig config;
    private FutureInstance<List<Server.Instance>> server;
    private FutureInstance<Client.Instance> client;
    private InetSocketAddress clientAddress;

    @BeforeAll
    protected void beforeAll() throws ExecutionException, InterruptedException {
        ClientConfig clientConfig = testConfig();
        config = clientConfig.getServers().getFirst();
        config.setTransport(new Transport[]{Transport.UDP});
        server = launchServer(clientConfig.getServers());
        client = launchClient(clientConfig);
        clientAddress = client.instance().tcp().localAddress();
    }

    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        updateConfig(config, parameter);
        handshakeAndSendBytes(clientAddress);
    }

    @AfterAll
    void afterAll() throws ExecutionException, InterruptedException {
        close(client, server);
    }
}
