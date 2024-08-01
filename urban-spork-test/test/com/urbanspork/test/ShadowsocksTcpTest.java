package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.server.Server;
import com.urbanspork.test.template.TcpTestTemplate;
import com.urbanspork.test.tool.TcpCapture;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.NetUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class ShadowsocksTcpTest extends TcpTestTemplate {
    private List<Server.Instance> server;
    private TcpCapture capture;
    private Client.Instance client;

    @Test
    void testTooShortHeader() throws InterruptedException, ExecutionException {
        setUp(true);
        Channel channel = connect(client.tcp().localAddress());
        String request = "GET http://" + NetUtil.toSocketAddressString(dstAddress) + "/?a=b&c=d HTTP/1.1\r\n\r\n";
        channel.writeAndFlush(Unpooled.wrappedBuffer(request.getBytes())).sync().addListener(ChannelFutureListener.CLOSE);
        byte[] msg = capture.nextOutboundCapture().getLast();
        SocketChannel remoteChannel = capture.newRemoteChannel();
        byte[] left = Arrays.copyOf(msg, 42);
        byte[] right = Arrays.copyOfRange(msg, 42, msg.length);
        remoteChannel.writeAndFlush(Unpooled.wrappedBuffer(left));
        Thread.sleep(Duration.ofMillis(100)); // flush output buffer
        remoteChannel.writeAndFlush(Unpooled.wrappedBuffer(right));
        Assertions.assertFalse(remoteChannel.isActive());
        remoteChannel.closeFuture().sync();
        ServerUserManager.DEFAULT.clear();
        closeServer(server);
        client.close();
    }

    @Test
    void testReplayAttack() throws ExecutionException, InterruptedException, TimeoutException {
        setUp(false);
        checkHttpSendBytes(client.tcp().localAddress());
        byte[] msg = capture.nextOutboundCapture().getLast();
        SocketChannel remoteChannel = capture.newRemoteChannel();
        byte[] header = Arrays.copyOf(msg, 75);
        remoteChannel.writeAndFlush(Unpooled.wrappedBuffer(header));
        Thread.sleep(Duration.ofMillis(100)); // flush output buffer
        int count = 75;
        for (int i = 75; i < msg.length; i++) {
            if (!remoteChannel.isWritable()) {
                break;
            }
            count++;
            remoteChannel.writeAndFlush(Unpooled.wrappedBuffer(new byte[]{msg[i]}));
        }
        Assertions.assertNotEquals(msg.length, count);
        remoteChannel.closeFuture().sync();
        ServerUserManager.DEFAULT.clear();
        closeServer(server);
        client.close();
    }

    void setUp(boolean block) throws ExecutionException, InterruptedException {
        ServerConfig serverConfig = ServerConfigTest.testConfig(serverPort);
        Protocol protocol = Protocol.shadowsocks;
        CipherKind cipher = CipherKind.aead2022_blake3_aes_256_gcm;
        serverConfig.setProtocol(protocol);
        serverConfig.setCipher(cipher);
        String clientPassword = getPropertyOrDefault("com.urbanspork.test.client.password", TestDice.rollPassword(protocol, cipher));
        String serverPassword = getPropertyOrDefault("com.urbanspork.test.server.password", TestDice.rollPassword(protocol, cipher));
        serverConfig.setPassword(serverPassword);
        List<ServerUserConfig> user = new ArrayList<>();
        user.add(new ServerUserConfig(TestDice.rollString(10), clientPassword));
        serverConfig.setUser(user);
        server = launchServer(List.of(serverConfig));
        capture = new TcpCapture(serverConfig.getPort(), block);
        ClientConfig config = ClientConfigTest.testConfig(clientPort, capture.getLocalChannel().localAddress().getPort());
        ServerConfig current = config.getCurrent();
        current.setCipher(serverConfig.getCipher());
        current.setProtocol(serverConfig.getProtocol());
        current.setPassword(serverPassword + ":" + clientPassword);
        client = launchClient(config);
    }

    private static String getPropertyOrDefault(String key, String defaultValue) {
        String property = System.getProperty(key);
        return property == null ? defaultValue : property;
    }
}
