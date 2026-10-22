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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.NetUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

class ShadowsocksTcpTest extends TcpTestTemplate {
    private static final Protocol PROTOCOL = Protocol.shadowsocks;
    private static final CipherKind CIPHER = CipherKind.aead2022_blake3_aes_256_gcm;

    @Test
    void testTooShortHeader() throws InterruptedException, ExecutionException {
        Credentials credentials = credentials();
        ServerConfig serverConfig = serverConfig(credentials);
        try (Server.Instances _ = launchServer(List.of(serverConfig));
             TcpCapture capture = new TcpCapture(serverConfig.getPort(), true);
             Client.Instance client = launchClient(clientConfig(capture, credentials))) {
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
        }
    }

    @Test
    void testReplayAttack() throws ExecutionException, InterruptedException, TimeoutException {
        Credentials credentials = credentials();
        ServerConfig serverConfig = serverConfig(credentials);
        try (Server.Instances _ = launchServer(List.of(serverConfig));
             TcpCapture capture = new TcpCapture(serverConfig.getPort(), false);
             Client.Instance client = launchClient(clientConfig(capture, credentials))) {
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
        }
    }

    private static ServerConfig serverConfig(Credentials credentials) {
        ServerConfig serverConfig = ServerConfigTest.testConfig(0);
        serverConfig.setProtocol(PROTOCOL);
        serverConfig.setCipher(CIPHER);
        serverConfig.setPassword(credentials.serverPassword());
        serverConfig.setUser(List.of(new ServerUserConfig(TestDice.rollString(10), credentials.clientPassword())));
        return serverConfig;
    }

    private static ClientConfig clientConfig(TcpCapture capture, Credentials credentials) {
        ClientConfig config = ClientConfigTest.testConfig(CLIENT_PORT, capture.getLocalChannel().localAddress().getPort());
        ServerConfig current = config.getCurrent();
        current.setCipher(CIPHER);
        current.setProtocol(PROTOCOL);
        current.setPassword(credentials.serverPassword() + ":" + credentials.clientPassword());
        return config;
    }

    private static Credentials credentials() {
        return new Credentials(
            getPropertyOrDefault("com.urbanspork.test.client.password", TestDice.rollPassword(PROTOCOL, CIPHER)),
            getPropertyOrDefault("com.urbanspork.test.server.password", TestDice.rollPassword(PROTOCOL, CIPHER))
        );
    }

    private static String getPropertyOrDefault(String key, String defaultValue) {
        String property = System.getProperty(key);
        return property == null ? defaultValue : property;
    }

    private record Credentials(String clientPassword, String serverPassword) {
    }
}
