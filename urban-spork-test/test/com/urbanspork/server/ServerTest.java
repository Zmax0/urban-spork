package com.urbanspork.server;

import com.urbanspork.common.Runtime;
import com.urbanspork.common.config.ConfigHandlerTest;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class ServerTest {
    private static Runtime runtime;

    @BeforeAll
    static void beforeAll() {
        runtime = new Runtime();
    }

    @Test
    void launchEmpty() throws NoSuchFieldException, IllegalAccessException {
        ConfigHandlerTest.runWithCustomJson("{\"servers\":[]}", Server::main);
    }

    @Test
    void launchFailed() {
        int port = TestDice.rollPort();
        List<ServerConfig> configs = ServerConfigTest.testConfigs(port, port);
        Assertions.assertThrows(RuntimeException.class, () -> Server.launch(runtime, configs));
    }

    @Test
    void launchEmptyQUIC() throws InterruptedException {
        ServerConfig quic = ServerConfigTest.testConfig(0);
        quic.setProtocol(Protocol.trojan);
        quic.setTransport(new Transport[]{Transport.QUIC});
        try (Server.Instances instances = Server.launch(runtime, Collections.singletonList(quic))) {
            Assertions.assertEquals(1, instances.servers().size());
        }
    }

    @Test
    void awaitTermination() throws Exception {
        Server.Instances instances = Server.launch(runtime, ServerConfigTest.testConfigs(0, 0));
        ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            Future<?> terminated = service.submit(() -> {
                instances.awaitTermination();
                return null;
            });
            instances.close();
            terminated.get(10, TimeUnit.SECONDS);
        } finally {
            service.shutdownNow();
            instances.close();
        }
    }

    @Test
    void sendInvalidUDP() throws InterruptedException, ExecutionException {
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setTransport(new Transport[]{Transport.TCP, Transport.UDP});
        try (Server.Instances _ = Server.launch(runtime, List.of(config))) {
            InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
            Channel channel = new Bootstrap().group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
                .channel(NioDatagramChannel.class)
                .handler(new LoggingHandler())
                .bind(0).syncUninterruptibly().channel();
            ChannelFuture future = channel.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(Dice.rollBytes(512)), serverAddress)).sync();
            future.get();
            Assertions.assertTrue(future.isDone());
        }
    }

    @Test
    void sendInvalidTCP() throws InterruptedException, ExecutionException {
        ServerConfig config = ServerConfigTest.testConfig(0);
        try (Server.Instances _ = Server.launch(runtime, List.of(config))) {
            Channel channel = new Bootstrap().group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler())
                .connect(new InetSocketAddress(config.getHost(), config.getPort())).syncUninterruptibly().channel();
            ChannelFuture future = channel.writeAndFlush(Unpooled.wrappedBuffer(Dice.rollBytes(512))).sync();
            future.get();
            Assertions.assertTrue(future.isDone());
        }
    }

    @AfterAll
    static void afterAll() {
        runtime.close();
    }
}
