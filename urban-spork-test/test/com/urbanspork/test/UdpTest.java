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
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.UdpTestTemplate;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

class UdpTest extends UdpTestTemplate {
    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        if (isShadowsocksAEAD2022Eih(parameter)) {
            testShadowsocksAEAD2022EihByParameter(parameter);
        }
        ClientConfig config = testConfig();
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setTransport(new Transport[]{Transport.UDP});
        serverConfig.setProtocol(parameter.protocol());
        serverConfig.setCipher(parameter.cipher());
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setSsl(parameter.sslSetting());
        serverConfig.setWs(parameter.wsSetting());
        try (Server.Instances _ = launchServer(config.getServers());
             Client.Instance client = launchClient(config)) {
            handshakeAndSendBytes(client.tcp().localAddress());
        }
    }

    void testShadowsocksAEAD2022EihByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        CipherKind cipher = parameter.cipher();
        Protocol protocol = parameter.protocol();
        Transport[] transports = {Transport.TCP, Transport.UDP};
        ServerConfig serverConfig = ServerConfigTest.testConfig(SERVER_PORT);
        serverConfig.setTransport(transports);
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setUser(List.of(new ServerUserConfig(TestDice.rollString(10), parameter.clientPassword())));
        try (Server.Instances _ = launchServer(List.of(serverConfig));
             Client.Instance client = launchClient(shadowsocksAEAD2022EihClientConfig(serverConfig, parameter))) {
            handshakeAndSendBytes(client.tcp().localAddress());
        }
    }

    @ParameterizedTest
    @ArgumentsSource(QuicProvider.class)
    void testQuicByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        ServerConfig serverConfig = ServerConfigTest.testConfig(SERVER_PORT);
        serverConfig.setTransport(new Transport[]{Transport.QUIC});
        serverConfig.setProtocol(parameter.protocol());
        serverConfig.setCipher(parameter.cipher());
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setSsl(parameter.sslSetting());
        serverConfig.setWs(parameter.wsSetting());
        try (Server.Instances _ = launchServer(List.of(serverConfig));
             Client.Instance client = launchClient(quicClientConfig(serverConfig, parameter))) {
            handshakeAndSendBytes(client.tcp().localAddress());
        }
    }

    private static ClientConfig shadowsocksAEAD2022EihClientConfig(ServerConfig serverConfig, Parameter parameter) {
        ClientConfig clientConfig = ClientConfigTest.testConfig(CLIENT_PORT, serverConfig.getPort());
        ServerConfig current = clientConfig.getCurrent();
        current.setCipher(parameter.cipher());
        current.setTransport(new Transport[]{Transport.TCP, Transport.UDP});
        current.setProtocol(parameter.protocol());
        current.setPassword(parameter.serverPassword() + ":" + parameter.clientPassword());
        return clientConfig;
    }

    private static ClientConfig quicClientConfig(ServerConfig serverConfig, Parameter parameter) {
        ClientConfig clientConfig = ClientConfigTest.testConfig(CLIENT_PORT, serverConfig.getPort());
        ServerConfig current = clientConfig.getCurrent();
        current.setCipher(parameter.cipher());
        current.setTransport(new Transport[]{Transport.UDP, Transport.QUIC});
        current.setProtocol(parameter.protocol());
        current.setSsl(parameter.sslSetting());
        current.setPassword(parameter.serverPassword());
        return clientConfig;
    }

    private static boolean isShadowsocksAEAD2022Eih(Parameter parameter) {
        CipherKind cipher = parameter.cipher();
        return parameter.protocol() == Protocol.shadowsocks && cipher != null && cipher.isAead2022() && cipher.supportEih();
    }

    static class QuicProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            return new Parameter.QuicProvider().provideArguments(parameters, context).flatMap(e -> Stream.of(e.get())).map(Parameter.class::cast)
                .filter(p -> p.protocol() != Protocol.shadowsocks).map(Arguments::of);
        }
    }
}
