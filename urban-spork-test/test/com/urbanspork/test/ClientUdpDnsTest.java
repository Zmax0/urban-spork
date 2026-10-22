package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.tcp.DohTestServer;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.UdpTestTemplate;
import io.netty.util.NetUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientUdpDnsTest extends UdpTestTemplate {
    @Test
    void testTcp() throws ExecutionException, InterruptedException, TimeoutException {
        DohTestServer dohServer = dohTestServer();
        Parameter parameter = newParameter();
        String nameServer = String.format("https://localhost:%d/dns-query?resolved=%s", dohServer.localAddress().getPort(), NetUtil.toAddressString(InetAddress.getLoopbackAddress()));
        InetSocketAddress dstAddress0 = dstAddress.set(0, new InetSocketAddress(TestDice.rollHost(), simpleEchoTestUdpServer.localAddress().getPort()));
        InetSocketAddress dstAddress1 = dstAddress.set(1, new InetSocketAddress(TestDice.rollHost(), delayedEchoTestUdpServer.localAddress().getPort()));
        try {
            DnsSetting dnsSetting = new DnsSetting(nameServer, parameter.sslSetting());
            ClientConfig config = testConfig();
            ServerConfig serverConfig = config.getServers().getFirst();
            serverConfig.setTransport(new Transport[]{Transport.UDP});
            serverConfig.setProtocol(parameter.protocol());
            serverConfig.setPassword(parameter.serverPassword());
            serverConfig.setSsl(parameter.sslSetting());
            serverConfig.setDns(dnsSetting);
            try (Server.Instances _ = launchServer(config.getServers());
                 Client.Instance client = launchClient(config)) {
                handshakeAndSendBytes(client.tcp().localAddress());
            }
        } finally {
            dstAddress.set(0, dstAddress0);
            dstAddress.set(1, dstAddress1);
        }
    }

    @Test
    void testQuic() throws ExecutionException, InterruptedException, TimeoutException {
        DohTestServer dohServer = dohTestServer();
        Parameter parameter = newParameter();
        String nameServer = String.format("https://localhost:%d/dns-query?resolved=%s", dohServer.localAddress().getPort(), NetUtil.toAddressString(InetAddress.getLoopbackAddress()));
        InetSocketAddress dstAddress0 = dstAddress.set(0, new InetSocketAddress(TestDice.rollHost(), simpleEchoTestUdpServer.localAddress().getPort()));
        InetSocketAddress dstAddress1 = dstAddress.set(1, new InetSocketAddress(TestDice.rollHost(), delayedEchoTestUdpServer.localAddress().getPort()));
        try {
            DnsSetting dnsSetting = new DnsSetting(nameServer, parameter.sslSetting());
            ServerConfig serverConfig = ServerConfigTest.testConfig(SERVER_PORT);
            serverConfig.setTransport(new Transport[]{Transport.QUIC});
            serverConfig.setProtocol(parameter.protocol());
            serverConfig.setPassword(parameter.serverPassword());
            serverConfig.setSsl(parameter.sslSetting());
            try (Server.Instances _ = launchServer(List.of(serverConfig));
                 Client.Instance client = launchClient(quicClientConfig(serverConfig, parameter, dnsSetting))) {
                handshakeAndSendBytes(client.tcp().localAddress());
            }
        } finally {
            dstAddress.set(0, dstAddress0);
            dstAddress.set(1, dstAddress1);
        }
    }

    private static ClientConfig quicClientConfig(ServerConfig serverConfig, Parameter parameter, DnsSetting dnsSetting) {
        ClientConfig clientConfig = ClientConfigTest.testConfig(CLIENT_PORT, serverConfig.getPort());
        ServerConfig current = clientConfig.getCurrent();
        current.setTransport(new Transport[]{Transport.UDP, Transport.QUIC});
        current.setProtocol(parameter.protocol());
        current.setSsl(parameter.sslSetting());
        current.setPassword(parameter.serverPassword());
        current.setDns(dnsSetting);
        return clientConfig;
    }

    private static Parameter newParameter() {
        Protocol protocol = Protocol.trojan;
        String password = TestDice.rollPassword(protocol, null);
        return new Parameter(protocol, null, password, password, null, SslUtil.getSslSetting());
    }
}
