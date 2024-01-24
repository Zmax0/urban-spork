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
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.server.Server;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.UDPTestTemplate;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@DisplayName("UDP")
class UDPTestCase extends UDPTestTemplate {
    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        Protocols protocol = parameter.protocol();
        CipherKind cipher = parameter.cipher();
        if (cipher.isAead2022() && cipher.supportEih()) {
            testShadowsocksAEAD2022EihByParameter(parameter);
        }
        ClientConfig config = ClientConfigTestCase.testConfig(0, 0);
        ServerConfig serverConfig = config.getServers().getFirst();
        Network[] networks = {Network.TCP, Network.UDP};
        serverConfig.setNetworks(networks);
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        List<Map.Entry<ServerSocketChannel, Optional<DatagramChannel>>> server = launchServer(config.getServers());
        Map.Entry<ServerSocketChannel, DatagramChannel> client = launchClient(config);
        InetSocketAddress clientLocalAddress = client.getKey().localAddress();
        handshakeAndSendBytes(clientLocalAddress);
        Server.close(server);
        Client.close(client);
    }

    void testShadowsocksAEAD2022EihByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        CipherKind cipher = parameter.cipher();
        Protocols protocol = parameter.protocol();
        Network[] networks = {Network.TCP, Network.UDP};
        ServerConfig serverConfig = ServerConfigTestCase.testConfig(0);
        serverConfig.setNetworks(networks);
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        List<ServerUserConfig> user = new ArrayList<>();
        user.add(new ServerUserConfig(TestDice.rollString(10), parameter.clientPassword()));
        serverConfig.setUser(user);
        List<Map.Entry<ServerSocketChannel, Optional<DatagramChannel>>> server = launchServer(List.of(serverConfig));
        ClientConfig clientConfig = ClientConfigTestCase.testConfig(0, serverConfig.getPort());
        ServerConfig current = clientConfig.getCurrent();
        current.setCipher(cipher);
        current.setNetworks(networks);
        current.setProtocol(protocol);
        current.setPassword(parameter.serverPassword() + ":" + parameter.clientPassword());
        Map.Entry<ServerSocketChannel, DatagramChannel> client = launchClient(clientConfig);
        InetSocketAddress clientLocalAddress = client.getKey().localAddress();
        handshakeAndSendBytes(clientLocalAddress);
        ServerUserManager.DEFAULT.clear();
        Server.close(server);
        Client.close(client);
    }
}
