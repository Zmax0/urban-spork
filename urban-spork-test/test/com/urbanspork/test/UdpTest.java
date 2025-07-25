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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

class UdpTest extends UdpTestTemplate {
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
        serverConfig.setTransport(new Transport[]{Transport.UDP});
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setSsl(parameter.sslSetting());
        serverConfig.setWs(parameter.wsSetting());
        List<Server.Instance> server = launchServer(config.getServers());
        Client.Instance client = launchClient(config);
        InetSocketAddress clientLocalAddress = client.tcp().localAddress();
        handshakeAndSendBytes(clientLocalAddress);
        closeServer(server);
        client.close();
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
        List<ServerUserConfig> user = new ArrayList<>();
        user.add(new ServerUserConfig(TestDice.rollString(10), parameter.clientPassword()));
        serverConfig.setUser(user);
        List<Server.Instance> server = launchServer(List.of(serverConfig));
        ClientConfig clientConfig = ClientConfigTest.testConfig(CLIENT_PORT, serverConfig.getPort());
        ServerConfig current = clientConfig.getCurrent();
        current.setCipher(cipher);
        current.setTransport(transports);
        current.setProtocol(protocol);
        current.setPassword(parameter.serverPassword() + ":" + parameter.clientPassword());
        Client.Instance client = launchClient(clientConfig);
        InetSocketAddress clientLocalAddress = client.tcp().localAddress();
        handshakeAndSendBytes(clientLocalAddress);
        closeServer(server);
        client.close();
    }

    @ParameterizedTest
    @ArgumentsSource(QuicProvider.class)
    void testQuicByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        Protocol protocol = parameter.protocol();
        CipherKind cipher = parameter.cipher();
        ServerConfig serverConfig = ServerConfigTest.testConfig(SERVER_PORT);
        serverConfig.setTransport(new Transport[]{Transport.QUIC});
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setSsl(parameter.sslSetting());
        serverConfig.setWs(parameter.wsSetting());
        List<Server.Instance> server = launchServer(List.of(serverConfig));
        ClientConfig clientConfig = ClientConfigTest.testConfig(CLIENT_PORT, serverConfig.getPort());
        ServerConfig current = clientConfig.getCurrent();
        current.setCipher(cipher);
        current.setTransport(new Transport[]{Transport.UDP, Transport.QUIC});
        current.setProtocol(protocol);
        current.setSsl(parameter.sslSetting());
        current.setPassword(parameter.serverPassword());
        Client.Instance client = launchClient(clientConfig);
        InetSocketAddress clientLocalAddress = client.tcp().localAddress();
        handshakeAndSendBytes(clientLocalAddress);
        closeServer(server);
        client.close();
    }

    static class QuicProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext extensionContext) {
            return new Parameter.QuicProvider().provideArguments(parameters, extensionContext).flatMap(e -> Stream.of(e.get())).map(Parameter.class::cast)
                .filter(p -> p.protocol() != Protocol.shadowsocks).map(Arguments::of);
        }
    }
}
