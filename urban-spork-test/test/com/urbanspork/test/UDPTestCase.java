package com.urbanspork.test;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.*;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.UDPTestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@DisplayName("UDP")
class UDPTestCase extends UDPTestTemplate {
    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        Protocols protocol = parameter.protocol();
        CipherKind cipher = parameter.cipher();
        if (cipher.isAead2022() && cipher.supportEih()) {
            testShadowsocksAEAD2022EihByParameter(parameter);
        }
        int[] ports = TestUtil.freePorts(2);
        ClientConfig config = ClientConfigTestCase.testConfig(ports[0], ports[1]);
        ServerConfig serverConfig = config.getServers().getFirst();
        Network[] networks = {Network.TCP, Network.UDP};
        serverConfig.setNetworks(networks);
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        Future<?> client = launchClient(service, executor, config);
        Future<?> server = launchServer(service, executor, config.getServers());
        for (int dstPort : dstPorts()) {
            handshakeAndSendBytes(config, dstPort);
        }
        client.cancel(true);
        server.cancel(true);
    }

    void testShadowsocksAEAD2022EihByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        int[] ports = TestUtil.freePorts(2);
        CipherKind cipher = parameter.cipher();
        Protocols protocol = parameter.protocol();
        Network[] networks = {Network.TCP, Network.UDP};
        ServerConfig serverConfig = ServerConfigTestCase.testConfig(ports[1]);
        serverConfig.setNetworks(networks);
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        List<ServerUserConfig> user = new ArrayList<>();
        user.add(new ServerUserConfig(TestDice.rollString(10), parameter.clientPassword()));
        serverConfig.setUser(user);
        Future<?> server = launchServer(service, executor, List.of(serverConfig));
        ClientConfig clientConfig = ClientConfigTestCase.testConfig(ports[0], ports[1]);
        ServerConfig current = clientConfig.getCurrent();
        current.setCipher(cipher);
        current.setNetworks(networks);
        current.setProtocol(protocol);
        current.setPassword(parameter.serverPassword() + ":" + parameter.clientPassword());
        Future<?> client = launchClient(service, executor, clientConfig);
        for (int dstPort : dstPorts()) {
            handshakeAndSendBytes(clientConfig, dstPort);
        }
        ServerUserManager.DEFAULT.clear();
        server.cancel(true);
        client.cancel(true);
    }
}
