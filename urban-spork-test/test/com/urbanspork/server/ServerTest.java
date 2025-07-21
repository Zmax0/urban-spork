package com.urbanspork.server;

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
import org.junit.jupiter.api.Assertions;
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerTest {

    @Test
    void launchRejected() {
        List<ServerConfig> empty = Collections.emptyList();
        Assertions.assertThrows(IllegalArgumentException.class, () -> Server.launch(empty));
    }

    @Test
    void launchFailed() {
        int port = TestDice.rollPort();
        List<ServerConfig> configs = ServerConfigTest.testConfigs(port, port);
        CompletableFuture<List<Server.Instance>> promise = new CompletableFuture<>();
        Server.launch(configs, promise);
        Assertions.assertEquals(java.net.BindException.class, promise.exceptionNow().getClass());
    }

    @Test
    void launchEmptyQUIC() throws ExecutionException, InterruptedException, TimeoutException {
        ServerConfig quic = ServerConfigTest.testConfig(0);
        quic.setProtocol(Protocol.trojan);
        quic.setTransport(new Transport[]{Transport.QUIC});
        CompletableFuture<List<Server.Instance>> promise = new CompletableFuture<>();
        try (ExecutorService service = Executors.newSingleThreadExecutor()) {
            service.submit(() -> Server.launch(Collections.singletonList(quic), promise));
            List<Server.Instance> res = promise.get(1, TimeUnit.SECONDS);
            Assertions.assertEquals(1, res.size());
            closeServer(res);
        }
    }

    @Test
    void shutdown() {
        List<ServerConfig> configs = ServerConfigTest.testConfigs(0, 0);
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
        ServerConfig config = ServerConfigTest.testConfig(0);
        config.setTransport(new Transport[]{Transport.TCP, Transport.UDP});
        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<List<Server.Instance>> promise = new CompletableFuture<>();
            service.submit(() -> Server.launch(List.of(config), promise));
            List<Server.Instance> servers = promise.get();
            InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
            Channel channel = new Bootstrap().group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
                .channel(NioDatagramChannel.class)
                .handler(new LoggingHandler())
                .bind(0).syncUninterruptibly().channel();
            ChannelFuture future = channel.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(Dice.rollBytes(512)), serverAddress)).sync();
            future.get();
            Assertions.assertTrue(future.isDone());
            closeServer(servers);
        }
    }

    @Test
    void sendInvalidTCP() throws InterruptedException, ExecutionException {
        ServerConfig config = ServerConfigTest.testConfig(0);
        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<List<Server.Instance>> promise = new CompletableFuture<>();
            service.submit(() -> Server.launch(List.of(config), promise));
            List<Server.Instance> servers = promise.get();
            Channel channel = new Bootstrap().group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler())
                .connect(new InetSocketAddress(config.getHost(), config.getPort())).syncUninterruptibly().channel();
            ChannelFuture future = channel.writeAndFlush(Unpooled.wrappedBuffer(Dice.rollBytes(512))).sync();
            future.get();
            Assertions.assertTrue(future.isDone());
            for (Server.Instance server : servers) {
                server.close();
            }
        }
    }

    private static void closeServer(List<Server.Instance> servers) {
        for (Server.Instance server : servers) {
            server.close();
        }
    }
}
