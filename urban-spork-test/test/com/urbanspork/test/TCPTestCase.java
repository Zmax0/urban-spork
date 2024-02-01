package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTestCase;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.server.Server;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.TCPTestTemplate;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@DisplayName("TCP")
class TCPTestCase extends TCPTestTemplate {
    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        Protocols protocol = parameter.protocol();
        CipherKind cipher = parameter.cipher();
        if (cipher.isAead2022() && cipher.supportEih()) {
            testShadowsocksAEAD2022EihByParameter(parameter);
        }
        ClientConfig config = ClientConfigTestCase.testConfig(0, 0);
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        List<Map.Entry<ServerSocketChannel, Optional<DatagramChannel>>> server = launchServer(config.getServers());
        Map.Entry<ServerSocketChannel, DatagramChannel> client = launchClient(config);
        handshakeAndSendBytes(client.getKey().localAddress());
        Server.close(server);
        Client.close(client);
    }

    void testShadowsocksAEAD2022EihByParameter(Parameter parameter) throws ExecutionException, InterruptedException {
        Protocols protocol = parameter.protocol();
        CipherKind cipher = parameter.cipher();
        ServerConfig serverConfig = ServerConfigTestCase.testConfig(0);
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        List<ServerUserConfig> user = new ArrayList<>();
        user.add(new ServerUserConfig(TestDice.rollString(10), parameter.clientPassword()));
        serverConfig.setUser(user);
        List<Map.Entry<ServerSocketChannel, Optional<DatagramChannel>>> server = launchServer(List.of(serverConfig));
        ClientConfig config = ClientConfigTestCase.testConfig(0, serverConfig.getPort());
        ServerConfig current = config.getCurrent();
        current.setCipher(cipher);
        current.setProtocol(protocol);
        current.setPassword(parameter.serverPassword() + ":" + parameter.clientPassword());
        Map.Entry<ServerSocketChannel, DatagramChannel> client = launchClient(config);
        handshakeAndSendBytes(client.getKey().localAddress());
        ServerUserManager.DEFAULT.clear();
        Server.close(server);
        Client.close(client);
    }
}
