package com.urbanspork.server;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.TestUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@DisplayName("Server")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerTestCase {

    @Test
    void launch() {
        List<ServerConfig> empty = Collections.emptyList();
        Assertions.assertThrows(IllegalArgumentException.class, () -> Server.launch(empty), "Server config in the file is empty");
        List<ServerConfig> configs = ServerConfigTestCase.testConfig(new int[]{TestDice.rollPort()});
        ServerConfig config = configs.getFirst();
        config.setHost("www.urban-spork.com");
        Assertions.assertThrows(IllegalArgumentException.class, () -> Server.launch(configs), "None available server");
    }

    @Test
    void launchFailed() throws InterruptedException {
        int port = TestDice.rollPort();
        List<ServerConfig> configs = ServerConfigTestCase.testConfig(new int[]{port, port});
        DefaultEventLoop executor = new DefaultEventLoop();
        Promise<List<ServerSocketChannel>> promise = executor.newPromise();
        Server.launch(configs, promise);
        promise.await(5, TimeUnit.SECONDS);
        Assertions.assertFalse(promise.isSuccess());
        executor.shutdownGracefully();
    }

    @Test
    void shutdown() {
        List<ServerConfig> configs = ServerConfigTestCase.testConfig(TestUtil.freePorts(2));
        Future<?> future;
        try (ExecutorService service = Executors.newSingleThreadExecutor()) {
            future = service.submit(() -> Server.launch(configs));
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
        List<ServerConfig> configs = ServerConfigTestCase.testConfig(TestUtil.freePorts(1));
        ServerConfig config = configs.getFirst();
        config.setNetworks(new Network[]{Network.TCP, Network.UDP});
        DefaultEventLoop executor = new DefaultEventLoop();
        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
            Promise<List<ServerSocketChannel>> promise = executor.newPromise();
            Future<?> server = service.submit(() -> Server.launch(configs, promise));
            promise.await().get();
            InetSocketAddress serverAddress = new InetSocketAddress(config.getHost(), config.getPort());
            Channel channel = new Bootstrap().group(new NioEventLoopGroup())
                .channel(NioDatagramChannel.class)
                .handler(new LoggingHandler())
                .bind(0).syncUninterruptibly().channel();
            ChannelFuture future = channel.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(Dice.rollBytes(512)), serverAddress)).sync();
            future.get();
            Assertions.assertTrue(future.isDone());
            executor.shutdownGracefully();
            server.cancel(true);
        }
    }

    @Test
    void sendInvalidTCP() throws InterruptedException, ExecutionException {
        List<ServerConfig> configs = ServerConfigTestCase.testConfig(TestUtil.freePorts(1));
        ServerConfig config = configs.getFirst();
        DefaultEventLoop executor = new DefaultEventLoop();
        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
            Promise<List<ServerSocketChannel>> promise = executor.newPromise();
            Future<?> server = service.submit(() -> Server.launch(configs, promise));
            promise.await().get();
            Channel channel = new Bootstrap().group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler())
                .connect(new InetSocketAddress(config.getHost(), config.getPort())).syncUninterruptibly().channel();
            ChannelFuture future = channel.writeAndFlush(Unpooled.wrappedBuffer(Dice.rollBytes(512))).sync();
            future.get();
            Assertions.assertTrue(future.isDone());
            executor.shutdownGracefully();
            server.cancel(true);
        }
    }
}
