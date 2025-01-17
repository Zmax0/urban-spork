package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.server.Server;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.TcpTestTemplate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class QuicTcpTest extends TcpTestTemplate {
    private static final Transport[] TRANSPORTS = new Transport[]{Transport.QUIC};

    @ParameterizedTest
    @ArgumentsSource(Parameter.QuicProvider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        CipherKind cipher = parameter.cipher();
        ClientConfig config = ClientConfigTest.testConfig(0, 0);
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(parameter.protocol());
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setTransport(TRANSPORTS);
        serverConfig.setSsl(parameter.sslSetting());
        List<Server.Instance> server = launchServer(config.getServers());
        Client.Instance client = launchClient(config);
        InetSocketAddress clientAddress = client.tcp().localAddress();
        socksHandshakeAndSendBytes(clientAddress);
        checkHttpsHandshakeAndSendBytes(clientAddress);
        checkHttpSendBytes(clientAddress);
        closeServer(server);
        client.close();
    }
}
