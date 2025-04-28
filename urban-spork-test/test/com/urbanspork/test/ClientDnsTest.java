package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.DnsSetting;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.SslSetting;
import com.urbanspork.common.config.WebSocketSetting;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.tcp.DohTestServer;
import com.urbanspork.test.template.TcpTestTemplate;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.DefaultPromise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientDnsTest extends TcpTestTemplate {
    @Test
    void test() throws ExecutionException, InterruptedException, TimeoutException {
        ServerSocketChannel dohServer = launchDohTestServer();
        Protocol protocol = Protocol.trojan;
        String password = TestDice.rollPassword(protocol, null);
        WebSocketSetting wsSetting = new WebSocketSetting();
        wsSetting.setPath("/ws");
        SslSetting sslSetting = SslUtil.getSslSetting();
        DnsSetting dnsSetting = new DnsSetting();
        dnsSetting.setSsl(sslSetting);
        InetSocketAddress echoServerAddress = echoTestServer.localAddress();
        this.dstAddress = new InetSocketAddress(TestDice.rollHost(), echoServerAddress.getPort());
        dnsSetting.setNameServer(String.format("https://localhost:%d/dns-query?resolved=%s", dohServer.localAddress().getPort(), NetUtil.toAddressString(echoServerAddress.getAddress())));
        ClientConfig config = testConfig();
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(protocol);
        serverConfig.setPassword(password);
        serverConfig.setTransport(new Transport[]{Transport.TCP});
        serverConfig.setSsl(sslSetting);
        serverConfig.setDns(dnsSetting);
        serverConfig.setWs(wsSetting);
        List<Server.Instance> server = launchServer(config.getServers());
        Client.Instance client = launchClient(config);
        InetSocketAddress clientTcpAddress = client.tcp().localAddress();
        socksHandshakeAndSendBytes(clientTcpAddress);
        socksHandshakeAndSendBytes(clientTcpAddress); // check dns cache
        closeServer(server);
        client.close();
        dohServer.close();
    }

    @Test
    void testErrorDns() throws ExecutionException, InterruptedException {
        Protocol protocol = Protocol.trojan;
        String password = TestDice.rollPassword(protocol, null);
        SslSetting sslSetting = SslUtil.getSslSetting();
        DnsSetting dnsSetting = new DnsSetting();
        ClientConfig config = testConfig();
        ServerConfig serverConfig = config.getServers().getFirst();
        serverConfig.setProtocol(protocol);
        serverConfig.setPassword(password);
        serverConfig.setTransport(new Transport[]{Transport.TCP});
        serverConfig.setSsl(sslSetting);
        serverConfig.setDns(dnsSetting);
        List<Server.Instance> server = launchServer(config.getServers());
        Client.Instance client = launchClient(config);
        this.dstAddress = InetSocketAddress.createUnresolved(TestDice.rollHost(), dstAddress.getPort());
        dnsSetting.setNameServer(String.format("https://localhost:%d/dns-query", echoTestServer.localAddress().getPort()));
        InetSocketAddress clientTcpAddress = client.tcp().localAddress();
        Assertions.assertThrows(ExecutionException.class, () -> socksHandshakeAndSendBytes(clientTcpAddress));
        closeServer(server);
        client.close();
    }

    static ServerSocketChannel launchDohTestServer() throws ExecutionException, InterruptedException {
        DefaultPromise<ServerSocketChannel> promise = new DefaultPromise<>() {};
        POOL.submit(() -> DohTestServer.launch(0, promise));
        return promise.get();
    }
}
