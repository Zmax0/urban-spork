package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTestCase;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.server.Server;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.TCPTestTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@DisplayName("TCP")
class TCPTestCase extends TCPTestTemplate {
    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        Protocol protocol = parameter.protocol();
        CipherKind cipher = parameter.cipher();
        if (cipher.isAead2022() && cipher.supportEih()) {
            testShadowsocksAEAD2022EihByParameter(parameter);
        }
        ClientConfig config = ClientConfigTestCase.testConfig(0, 0);
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        List<Server.Instance> server = launchServer(config.getServers());
        Client.Instance client = launchClient(config);
        handshakeAndSendBytes(client.tcp().localAddress());
        closeServer(server);
        client.close();
    }

    @Test
    void testConnectServerFailed() throws ExecutionException, InterruptedException {
        ClientConfig config = ClientConfigTestCase.testConfig(0, TestDice.rollPort());
        Client.Instance client = launchClient(config);
        Assertions.assertThrows(ExecutionException.class, () -> handshakeAndSendBytes(client.tcp().localAddress()));
        client.close();
    }

    void testShadowsocksAEAD2022EihByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        Protocol protocol = parameter.protocol();
        CipherKind cipher = parameter.cipher();
        ServerConfig serverConfig = ServerConfigTestCase.testConfig(0);
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        List<ServerUserConfig> user = new ArrayList<>();
        user.add(new ServerUserConfig(TestDice.rollString(10), parameter.clientPassword()));
        serverConfig.setUser(user);
        List<Server.Instance> server = launchServer(List.of(serverConfig));
        ClientConfig config = ClientConfigTestCase.testConfig(0, serverConfig.getPort());
        ServerConfig current = config.getCurrent();
        current.setCipher(cipher);
        current.setProtocol(protocol);
        current.setPassword(parameter.serverPassword() + ":" + parameter.clientPassword());
        Client.Instance client = launchClient(config);
        handshakeAndSendBytes(client.udp().localAddress());
        ServerUserManager.DEFAULT.clear();
        closeServer(server);
        client.close();
    }
}
