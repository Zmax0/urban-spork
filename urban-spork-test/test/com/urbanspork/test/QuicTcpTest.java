package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.server.Server;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.TcpTestTemplate;
import io.netty.channel.socket.ServerSocketChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuicTcpTest extends TcpTestTemplate {
    private static final Transport[] TRANSPORTS = new Transport[]{Transport.QUIC};

    @Order(1)
    @ParameterizedTest
    @ArgumentsSource(Parameter.QuicProvider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        CipherKind cipher = parameter.cipher();
        ClientConfig config = testConfig();
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

    @Order(2)
    @ParameterizedTest
    @ArgumentsSource(Parameter.QuicProvider.class)
    void testClientDnsByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        ServerSocketChannel dohServer = ClientDnsTest.launchDohTestServer();
        CipherKind cipher = parameter.cipher();
        ClientConfig config = testConfig();
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(parameter.protocol());
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setTransport(TRANSPORTS);
        serverConfig.setSsl(parameter.sslSetting());
        DnsSetting dnsSetting = new DnsSetting();
        dnsSetting.setSsl(parameter.sslSetting());
        InetSocketAddress echoServerAddress = echoTestServer.localAddress();
        this.dstAddress = new InetSocketAddress(TestDice.rollHost(), echoServerAddress.getPort());
        dnsSetting.setNameServer(String.format("https://localhost:%d?&resolved=%s&name=", dohServer.localAddress().getPort(), echoServerAddress.getHostString()));
        serverConfig.setDns(dnsSetting);
        List<Server.Instance> server = launchServer(config.getServers());
        Client.Instance client = launchClient(config);
        InetSocketAddress clientAddress = client.tcp().localAddress();
        socksHandshakeAndSendBytes(clientAddress);
        socksHandshakeAndSendBytes(clientAddress); // check dns cache
        closeServer(server);
        client.close();
    }

    @Order(3)
    @Test
    void testClientErrorDns() throws ExecutionException, InterruptedException {
        Protocol protocol = Protocol.trojan;
        String password = TestDice.rollPassword(protocol, null);
        SslSetting sslSetting = SslUtil.getSslSetting();
        DnsSetting dnsSetting = new DnsSetting();
        ClientConfig config = testConfig();
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(protocol);
        serverConfig.setPassword(password);
        serverConfig.setTransport(TRANSPORTS);
        serverConfig.setSsl(sslSetting);
        serverConfig.setDns(dnsSetting);
        List<Server.Instance> server = launchServer(config.getServers());
        Client.Instance client = launchClient(config);
        this.dstAddress = InetSocketAddress.createUnresolved(TestDice.rollHost(), echoTestServer.localAddress().getPort());
        dnsSetting.setNameServer(String.format("https://localhost:%d?&&name=", dstAddress.getPort()));
        InetSocketAddress clientTcpAddress = client.tcp().localAddress();
        Assertions.assertThrows(ExecutionException.class, () -> socksHandshakeAndSendBytes(clientTcpAddress));
        closeServer(server);
        client.close();
    }
}
