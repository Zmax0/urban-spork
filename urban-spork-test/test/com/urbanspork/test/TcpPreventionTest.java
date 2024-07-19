package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.server.Server;
import com.urbanspork.test.template.TcpTestTemplate;
import com.urbanspork.test.tool.TcpCapture;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.NetUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class TcpPreventionTest extends TcpTestTemplate {
    private List<Server.Instance> server;
    private TcpCapture capture;
    private Client.Instance client;

    @Test
    void testTooShortHeader() throws InterruptedException, ExecutionException {
        setUp(true);
        Channel channel = connect(client.tcp().localAddress());
        String request = "GET http://" + NetUtil.toSocketAddressString(dstAddress) + "/?a=b&c=d HTTP/1.1\r\n\r\n";
        channel.writeAndFlush(Unpooled.wrappedBuffer(request.getBytes())).sync();
        byte[] msg = capture.nextOutboundCapture().getLast();
        capture.send(msg, (c, m) -> {
            byte[] header = Arrays.copyOf(m, 42);
            c.writeAndFlush(Unpooled.wrappedBuffer(header));
            int i = 42;
            for (; i < m.length; i++) {
                if (!c.isActive()) {
                    c.writeAndFlush(Unpooled.wrappedBuffer(new byte[]{msg[i]}));
                }
            }
            Assertions.assertEquals(msg.length, i);
        });
        closeServer(server);
        client.close();
    }

    @Test
    void testReplayAttack() throws ExecutionException, InterruptedException, TimeoutException {
        setUp(false);
        checkHttpSendBytes(client.tcp().localAddress());
        byte[] msg = capture.nextOutboundCapture().getLast();
        capture.send(msg, (c, m) -> {
            byte[] header = Arrays.copyOf(m, 75);
            c.writeAndFlush(Unpooled.wrappedBuffer(header));
            int count = 75;
            for (int i = 75; i < m.length; i++) {
                if (c.isWritable()) {
                    count++;
                    c.writeAndFlush(Unpooled.wrappedBuffer(new byte[]{m[i]}));
                } else {
                    break;
                }
            }
            Assertions.assertEquals(msg.length, count);
        });
        closeServer(server);
        client.close();
    }

    void setUp(boolean block) throws ExecutionException, InterruptedException {
        ServerConfig serverConfig = ServerConfigTest.testConfig(0);
        Protocol protocol = Protocol.shadowsocks;
        CipherKind cipher = CipherKind.aead2022_blake3_aes_256_gcm;
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        String clientPassword = TestDice.rollPassword(protocol, cipher);
        String serverPassword = TestDice.rollPassword(protocol, cipher);
        serverConfig.setPassword(serverPassword);
        List<ServerUserConfig> user = new ArrayList<>();
        user.add(new ServerUserConfig(TestDice.rollString(10), clientPassword));
        serverConfig.setUser(user);
        server = launchServer(List.of(serverConfig));
        capture = new TcpCapture(serverConfig.getPort(), block);
        ClientConfig config = ClientConfigTest.testConfig(0, capture.getLocalChannel().localAddress().getPort());
        ServerConfig current = config.getCurrent();
        current.setCipher(serverConfig.getCipher());
        current.setProtocol(serverConfig.getProtocol());
        current.setPassword(serverPassword + ":" + clientPassword);
        client = launchClient(config);
    }
}
