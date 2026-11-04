package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.client.ClientChannelContext;
import com.urbanspork.client.ClientTcpRelayHandler;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.tcp.DohTestServer;
import com.urbanspork.test.server.tcp.EchoTestServer;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.TcpTestTemplate;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.NetUtil;
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
        ClientConfig config = testConfig();
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(parameter.protocol());
        serverConfig.setCipher(parameter.cipher());
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setTransport(TRANSPORTS);
        serverConfig.setSsl(parameter.sslSetting());
        try (Server.Instances _ = launchServer(config.getServers());
             Client.Instance client = launchClient(config)) {
            InetSocketAddress clientAddress = client.tcp().localAddress();
            socksHandshakeAndSendBytes(clientAddress);
            checkHttpsHandshakeAndSendBytes(clientAddress);
            checkHttpSendBytes(clientAddress);
        }
    }

    @Order(2)
    @ParameterizedTest
    @ArgumentsSource(Parameter.QuicProvider.class)
    void testClientDnsByParameter(Parameter parameter) throws ExecutionException, InterruptedException, TimeoutException {
        DohTestServer dohServer = dohTestServer();
        ClientConfig config = testConfig();
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(parameter.protocol());
        serverConfig.setCipher(parameter.cipher());
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setTransport(TRANSPORTS);
        serverConfig.setSsl(parameter.sslSetting());
        InetSocketAddress echoServerAddress = echoTestServer.localAddress();
        String nameServer = String.format("https://localhost:%d?&resolved=%s&name=", dohServer.localAddress().getPort(), NetUtil.toAddressString(echoServerAddress.getAddress()));
        DnsSetting dnsSetting = new DnsSetting(nameServer, parameter.sslSetting());
        this.dstAddress = new InetSocketAddress(TestDice.rollHost(), echoServerAddress.getPort());
        serverConfig.setDns(dnsSetting);
        try (Server.Instances _ = launchServer(config.getServers());
             Client.Instance client = launchClient(config)) {
            InetSocketAddress clientAddress = client.tcp().localAddress();
            socksHandshakeAndSendBytes(clientAddress);
            socksHandshakeAndSendBytes(clientAddress); // check dns cache
        }
    }

    @Order(3)
    @Test
    void testClientErrorDns() throws ExecutionException, InterruptedException {
        Protocol protocol = Protocol.trojan;
        String password = TestDice.rollPassword(protocol, null);
        SslSetting sslSetting = SslUtil.getSslSetting();
        String nameServer = String.format("https://localhost:%d?&&name=", dstAddress.getPort());
        DnsSetting dnsSetting = new DnsSetting(nameServer, null);
        ClientConfig config = testConfig();
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(protocol);
        serverConfig.setPassword(password);
        serverConfig.setTransport(TRANSPORTS);
        serverConfig.setSsl(sslSetting);
        serverConfig.setDns(dnsSetting);
        try (Server.Instances _ = launchServer(config.getServers());
             Client.Instance client = launchClient(config)) {
            this.dstAddress = InetSocketAddress.createUnresolved(TestDice.rollHost(), echoTestServer.localAddress().getPort());
            InetSocketAddress clientTcpAddress = client.tcp().localAddress();
            Assertions.assertThrows(ExecutionException.class, () -> socksHandshakeAndSendBytes(clientTcpAddress));
        }
    }

    @Order(4)
    @Test
    void testCreateQuicStreamFailed() throws Exception {
        ServerConfig config = ServerConfigTest.testConfig(SERVER_PORT);
        config.setSsl(SslUtil.getSslSetting());
        config.setProtocol(Protocol.trojan);
        config.setTransport(new Transport[]{Transport.QUIC});
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try (Server.Instances servers = launchServer(List.of(config));
             EchoTestServer _ = EchoTestServer.launch(0)) {
            InetSocketAddress serverAddress = servers.servers().getFirst().tcp().localAddress();
            Channel inbound = new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(new ChannelInboundHandlerAdapter()).bind(0).sync().channel();
            new ClientTcpRelayHandler() {}.connect(inbound, serverAddress, new ClientChannelContext(config, null, null));
            inbound.close().sync();
        } finally {
            group.shutdownGracefully().syncUninterruptibly();
        }
    }
}
