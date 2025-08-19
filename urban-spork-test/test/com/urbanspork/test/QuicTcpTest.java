package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.client.ClientChannelContext;
import com.urbanspork.client.ClientTcpRelayHandler;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.tcp.EchoTestServer;
import com.urbanspork.test.template.Parameter;
import com.urbanspork.test.template.TcpTestTemplate;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

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
        ServerSocketChannel dohServer = dohTestServer();
        CipherKind cipher = parameter.cipher();
        ClientConfig config = testConfig();
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(parameter.protocol());
        serverConfig.setCipher(cipher);
        serverConfig.setPassword(parameter.serverPassword());
        serverConfig.setTransport(TRANSPORTS);
        serverConfig.setSsl(parameter.sslSetting());
        InetSocketAddress echoServerAddress = echoTestServer.localAddress();
        String nameServer = String.format("https://localhost:%d?&resolved=%s&name=", dohServer.localAddress().getPort(), NetUtil.toAddressString(echoServerAddress.getAddress()));
        DnsSetting dnsSetting = new DnsSetting(nameServer, parameter.sslSetting());
        this.dstAddress = new InetSocketAddress(TestDice.rollHost(), echoServerAddress.getPort());
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
        String nameServer = String.format("https://localhost:%d?&&name=", dstAddress.getPort());
        DnsSetting dnsSetting = new DnsSetting(nameServer, null);
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
        InetSocketAddress clientTcpAddress = client.tcp().localAddress();
        Assertions.assertThrows(ExecutionException.class, () -> socksHandshakeAndSendBytes(clientTcpAddress));
        closeServer(server);
        client.close();
    }

    @Order(4)
    @Test
    void testCreateQuicStreamFailed() throws Exception {
        ServerConfig config = ServerConfigTest.testConfig(SERVER_PORT);
        config.setSsl(SslUtil.getSslSetting());
        config.setProtocol(Protocol.trojan);
        config.setTransport(new Transport[]{Transport.QUIC});
        List<Server.Instance> server = launchServer(Collections.singletonList(config));
        InetSocketAddress serverAddress = server.getFirst().tcp().localAddress();
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            Future<?> submitted = pool.submit(() -> EchoTestServer.launch(0, new CompletableFuture<>()));
            Channel inbound = new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(new ChannelInboundHandlerAdapter()).bind(0).sync().channel();
            inbound.attr(ClientChannelContext.KEY).set(new ClientChannelContext(config, null, null));
            new ClientTcpRelayHandler() {}.connect(inbound, serverAddress);
            inbound.close().sync();
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
            submitted.cancel(true);
        }
        closeServer(server);
        group.shutdownGracefully();
    }
}
