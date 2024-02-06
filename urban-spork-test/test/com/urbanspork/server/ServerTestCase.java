package com.urbanspork.server;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

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

@DisplayName("Server")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerTestCase {

    @Test
    void launchRejected() {
        List<ServerConfig> empty = Collections.emptyList();
        Assertions.assertThrows(IllegalArgumentException.class, () -> Server.launch(empty));
        List<ServerConfig> configs = ServerConfigTestCase.testConfigs(TestDice.rollPort());
        ServerConfig config = configs.getFirst();
        config.setHost("www.urban-spork.com");
        Assertions.assertThrows(IllegalArgumentException.class, () -> Server.launch(configs));
    }

    @Test
    void launchFailed() {
        int port = TestDice.rollPort();
        List<ServerConfig> configs = ServerConfigTestCase.testConfigs(port, port);
        CompletableFuture<List<Server.Instance>> promise = new CompletableFuture<>();
        Server.launch(configs, promise);
        Assertions.assertTrue(promise.isCompletedExceptionally());
    }

    @Test
    void shutdown() {
        List<ServerConfig> configs = ServerConfigTestCase.testConfigs(0, 0);
        try (ExecutorService service = Executors.newSingleThreadExecutor()) {
            Future<?> future = service.submit(() -> Server.launch(configs));
            try {
                future.get(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException e) {
                future.cancel(true);
            }
            Assertions.assertTrue(future.isCancelled());
        }
    }

    @Test
    void sendInvalidUDP() throws InterruptedException, ExecutionException {
        ServerConfig config = ServerConfigTestCase.testConfig(0);
        config.setTransport(new Transport[]{Transport.TCP, Transport.UDP});
        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<List<Server.Instance>> promise = new CompletableFuture<>();
            service.submit(() -> Server.launch(List.of(config), promise));
            List<Server.Instance> servers = promise.get();
            InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
            Channel channel = new Bootstrap().group(new NioEventLoopGroup())
                .channel(NioDatagramChannel.class)
                .handler(new LoggingHandler())
                .bind(0).syncUninterruptibly().channel();
            ChannelFuture future = channel.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(Dice.rollBytes(512)), serverAddress)).sync();
            future.get();
            Assertions.assertTrue(future.isDone());
            for (Server.Instance server : servers) {
                server.close();
            }
        }
    }

    @Test
    void sendInvalidTCP() throws InterruptedException, ExecutionException {
        ServerConfig config = ServerConfigTestCase.testConfig(0);
        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<List<Server.Instance>> promise = new CompletableFuture<>();
            Future<?> server = service.submit(() -> Server.launch(List.of(config), promise));
            promise.get();
            Channel channel = new Bootstrap().group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler())
                .connect(new InetSocketAddress(config.getHost(), config.getPort())).syncUninterruptibly().channel();
            ChannelFuture future = channel.writeAndFlush(Unpooled.wrappedBuffer(Dice.rollBytes(512))).sync();
            future.get();
            Assertions.assertTrue(future.isDone());
            server.cancel(true);
        }
    }
}
