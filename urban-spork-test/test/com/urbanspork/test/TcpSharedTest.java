package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.server.Server;
import com.urbanspork.test.template.FutureInstance;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.TcpTestTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class TcpSharedTest extends TcpTestTemplate {
    private static final Transport[] TRANSPORTS = new Transport[]{Transport.TCP};

    private ServerConfig config;
    private FutureInstance<List<Server.Instance>> server;
    private FutureInstance<Client.Instance> client;
    private InetSocketAddress clientAddress;

    private ServerConfig shadowsocksAEAD2022EihServerConfig;
    private ClientConfig shadowsocksAEAD2022EihClientConfig;
    private FutureInstance<List<Server.Instance>> shadowsocksAEAD2022EihServer;
    private ServerUserManager shadowsocksAEAD2022EihServerUserManager;
    private FutureInstance<Client.Instance> shadowsocksAEAD2022EihClient;
    private InetSocketAddress shadowsocksAEAD2022EihClientAddress;

    @BeforeAll
    void beforeAll() throws ExecutionException, InterruptedException {
        init();
        initShadowsocksAEAD2022Eih();
    }

    private void init() throws InterruptedException, ExecutionException {
        ClientConfig clientConfig = testConfig();
        config = clientConfig.getServers().getFirst();
        config.setTransport(TRANSPORTS);
        server = launchServer(clientConfig.getServers());
        client = launchClient(clientConfig);
        clientAddress = client.instance().tcp().localAddress();
    }

    private void initShadowsocksAEAD2022Eih() throws InterruptedException, ExecutionException {
        shadowsocksAEAD2022EihServerConfig = ServerConfigTest.testConfig(0);
        shadowsocksAEAD2022EihServerConfig.setTransport(TRANSPORTS);
        shadowsocksAEAD2022EihServer = launchServer(List.of(shadowsocksAEAD2022EihServerConfig));
        shadowsocksAEAD2022EihServerUserManager = shadowsocksAEAD2022EihServer.instance().getFirst().context().userManager();
        shadowsocksAEAD2022EihClientConfig = ClientConfigTest.testConfig(0, shadowsocksAEAD2022EihServerConfig.getPort());
        shadowsocksAEAD2022EihClientConfig.getCurrent().setTransport(TRANSPORTS);
        shadowsocksAEAD2022EihClient = launchClient(shadowsocksAEAD2022EihClientConfig);
        shadowsocksAEAD2022EihClientAddress = shadowsocksAEAD2022EihClient.instance().tcp().localAddress();
    }

    @ParameterizedTest
    @ArgumentsSource(Parameter.Provider.class)
    void testByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        updateConfig(config, parameter);
        CipherKind cipher = parameter.cipher();
        Protocol protocol = parameter.protocol();
        if (protocol == Protocol.shadowsocks && cipher.isAead2022() && cipher.supportEih()) {
            shadowsocksAEAD2022EihServerConfig.setProtocol(protocol);
            shadowsocksAEAD2022EihServerConfig.setCipher(cipher);
            shadowsocksAEAD2022EihServerConfig.setPassword(parameter.serverPassword());
            shadowsocksAEAD2022EihServerUserManager.clear();
            shadowsocksAEAD2022EihServerUserManager.addUser(ServerUser.from(new ServerUserConfig(TestDice.rollString(10), parameter.clientPassword())));
            ServerConfig current = shadowsocksAEAD2022EihClientConfig.getCurrent();
            current.setProtocol(protocol);
            current.setCipher(cipher);
            current.setPassword(parameter.serverPassword() + ":" + parameter.clientPassword());
            socksHandshakeAndSendBytes(shadowsocksAEAD2022EihClientAddress);
            checkHttpsHandshakeAndSendBytes(shadowsocksAEAD2022EihClientAddress);
            checkHttpSendBytes(shadowsocksAEAD2022EihClientAddress);
        }
        socksHandshakeAndSendBytes(clientAddress);
        checkHttpsHandshakeAndSendBytes(clientAddress);
        checkHttpSendBytes(clientAddress);
    }

    @AfterAll
    protected void afterAll() {
        closeClient(client);
        closeServer(server);
        closeClient(shadowsocksAEAD2022EihClient);
        closeServer(shadowsocksAEAD2022EihServer);
    }
}
