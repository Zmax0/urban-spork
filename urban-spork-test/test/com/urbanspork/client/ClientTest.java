package com.urbanspork.client;

import com.urbanspork.common.Runtime;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.test.client.Socks5Handshake;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientTest {
    private static final Runtime RUNTIME = new Runtime();

    private final EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    @Test
    @Order(1)
    void testReload() throws InterruptedException, ExecutionException {
        ClientConfig clientConfig = ClientConfigTest.testConfig(0, 0);
        try (Client.Instance client = Client.launch(RUNTIME, clientConfig).get()) {
            ServerConfig current = clientConfig.getCurrent();
            Client.reload(RUNTIME, client, current);
            Assertions.assertTrue(client.tcp().isActive());
        }
    }

    @Test
    @Order(2)
    void testExit() throws InterruptedException, ExecutionException, TimeoutException {
        ClientConfig config = ClientConfigTest.testConfig(0, 0);
        ConfigHandler.DEFAULT.save(config);
        try (Client.Instance client = Client.launch(RUNTIME, config).get();
             ExecutorService pool = Executors.newSingleThreadExecutor()) {
            Future<?> terminated = pool.submit(() -> {
                client.awaitTermination();
                return null;
            });
            client.close();
            terminated.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(3)
    void testLaunchFailed() throws InterruptedException, ExecutionException {
        Client.Instance client = launch(ClientConfigTest.testConfig(0, 0));
        ClientConfig config = ClientConfigTest.testConfig(client.tcp().localAddress().getPort(), 0);
        config.setHost(null);
        Assertions.assertThrows(
            ExecutionException.class, () -> {
                Client.Instance client2 = launch(config);
                client2.close();
            }
        );
        client.close();
    }

    @ParameterizedTest
    @ArgumentsSource(Socks5CommandTypeProvider.class)
    @Order(4)
    void testHandshake(Socks5CommandType type) {
        InetSocketAddress proxyAddress = new InetSocketAddress(0);
        InetSocketAddress dstAddress = new InetSocketAddress(0);
        Assertions.assertThrows(ExecutionException.class, () -> Socks5Handshake.noAuth(group, type, proxyAddress, dstAddress).get(10, TimeUnit.SECONDS));
    }

    public static Client.Instance launch(ClientConfig config) throws InterruptedException, ExecutionException {
        return Client.launch(RUNTIME, config).get();
    }

    private static class Socks5CommandTypeProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext extensionContext) {
            Socks5CommandType[] types = {Socks5CommandType.CONNECT, Socks5CommandType.BIND};
            return Arrays.stream(types).map(Arguments::of);
        }
    }
}
