package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.server.Server;
import com.urbanspork.test.template.FutureInstance;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.TcpTestTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class TcpTest extends TcpTestTemplate {
    private static final Transport[] TRANSPORTS = new Transport[]{Transport.TCP};

    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        Protocol protocol = parameter.protocol();
        CipherKind cipher = parameter.cipher();
        if (protocol == Protocol.shadowsocks && cipher.isAead2022() && cipher.supportEih()) {
            testShadowsocksAEAD2022EihByParameter(parameter);
        }
        ClientConfig config = testConfig();
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setTransport(TRANSPORTS);
        serverConfig.setSsl(parameter.sslSetting());
        serverConfig.setWs(parameter.wsSetting());
        FutureInstance<List<Server.Instance>> server = launchServer(config.getServers());
        FutureInstance<Client.Instance> client = launchClient(config);
        InetSocketAddress clientAddress = client.instance().tcp().localAddress();
        socksHandshakeAndSendBytes(clientAddress);
        checkHttpsHandshakeAndSendBytes(clientAddress);
        checkHttpSendBytes(clientAddress);
        closeServer(server);
        closeClient(client);
    }

    @Test
    void testHttpBadRequest() throws ExecutionException, InterruptedException {
        ClientConfig config = testConfig();
        FutureInstance<Client.Instance> client = launchClient(config);
        InetSocketAddress proxyAddress = client.instance().udp().localAddress();
        Assertions.assertThrows(ExecutionException.class, () -> checkHttpSendBytes(proxyAddress, proxyAddress));
        client.instance().close();
    }

    @Test
    void testConnectServerFailed() throws ExecutionException, InterruptedException {
        ClientConfig config = ClientConfigTest.testConfig(CLIENT_PORT, TestDice.rollPort());
        FutureInstance<Client.Instance> client = launchClient(config);
        InetSocketAddress clientAddress = client.instance().tcp().localAddress();
        Assertions.assertThrows(ExecutionException.class, () -> socksHandshakeAndSendBytes(clientAddress));
        Assertions.assertThrows(ExecutionException.class, () -> checkHttpsHandshakeAndSendBytes(clientAddress));
        Assertions.assertThrows(ExecutionException.class, () -> checkHttpSendBytes(clientAddress));
        closeClient(client);
    }

    void testShadowsocksAEAD2022EihByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        Protocol protocol = parameter.protocol();
        CipherKind cipher = parameter.cipher();
        ServerConfig serverConfig = ServerConfigTest.testConfig(0);
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setTransport(TRANSPORTS);
        List<ServerUserConfig> user = new ArrayList<>();
        user.add(new ServerUserConfig(TestDice.rollString(10), parameter.clientPassword()));
        serverConfig.setUser(user);
        FutureInstance<List<Server.Instance>> server = launchServer(List.of(serverConfig));
        ClientConfig config = ClientConfigTest.testConfig(0, serverConfig.getPort());
        ServerConfig current = config.getCurrent();
        current.setCipher(cipher);
        current.setProtocol(protocol);
        current.setPassword(parameter.serverPassword() + ":" + parameter.clientPassword());
        FutureInstance<Client.Instance> client = launchClient(config);
        InetSocketAddress clientAddress = client.instance().tcp().localAddress();
        socksHandshakeAndSendBytes(clientAddress);
        checkHttpsHandshakeAndSendBytes(clientAddress);
        checkHttpSendBytes(clientAddress);
        closeServer(server);
        closeClient(client);
    }
}
