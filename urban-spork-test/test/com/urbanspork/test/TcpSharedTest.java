package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.protocol.Protocol;
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

class TcpSharedTest extends TcpTestTemplate {
    private static final Transport[] TRANSPORTS = new Transport[]{Transport.TCP};

    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        if (isShadowsocksAEAD2022Eih(parameter)) {
            testByParameter(parameter, true);
        }
        testByParameter(parameter, false);
    }

    private void testByParameter(Parameter parameter, boolean eih) throws ExecutionException, InterruptedException, TimeoutException {
        ServerConfig serverConfig = testConfig().getCurrent();
        serverConfig.setTransport(TRANSPORTS);
        updateConfig(serverConfig, parameter);
        if (eih) {
            serverConfig.setUser(List.of(new ServerUserConfig(TestDice.rollString(10), parameter.clientPassword())));
        }
        try (Server.Instances _ = launchServer(List.of(serverConfig));
             Client.Instance client = launchClient(clientConfig(serverConfig, parameter, eih))) {
            InetSocketAddress address = client.tcp().localAddress();
            socksHandshakeAndSendBytes(address);
            checkHttpsHandshakeAndSendBytes(address);
            checkHttpSendBytes(address);
        }
    }

    private static ClientConfig clientConfig(ServerConfig serverConfig, Parameter parameter, boolean eih) {
        ClientConfig clientConfig = ClientConfigTest.testConfig(0, serverConfig.getPort());
        ServerConfig current = clientConfig.getCurrent();
        current.setTransport(TRANSPORTS);
        updateConfig(current, parameter);
        if (eih) {
            current.setPassword(parameter.serverPassword() + ":" + parameter.clientPassword());
        }
        return clientConfig;
    }

    private static boolean isShadowsocksAEAD2022Eih(Parameter parameter) {
        CipherKind cipher = parameter.cipher();
        return parameter.protocol() == Protocol.shadowsocks && cipher != null && cipher.isAead2022() && cipher.supportEih();
    }
}
