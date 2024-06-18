package com.urbanspork.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.protocol.socks.ClientHandshake;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
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

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
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
    private static final ExecutorService SERVICE = Executors.newVirtualThreadPerTaskExecutor();
    private final EventLoopGroup group = new NioEventLoopGroup();

    @Test
    @Order(1)
    void testExit() {
        ClientConfig config = ClientConfigTest.testConfig(0, 0);
        ConfigHandler.DEFAULT.save(config);
        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            Future<?> future = pool.submit(() -> Client.main(null));
            try {
                future.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                if (e instanceof TimeoutException) {
                    future.cancel(true);
                } else {
                    throw new RuntimeException(e);
                }
            }
            Assertions.assertTrue(future.isCancelled());
        }
    }

    @Test
    @Order(2)
    void testLaunchFailed() throws InterruptedException, ExecutionException {
        Client.Instance client = asyncLaunchClient(ClientConfigTest.testConfig(0, 0));
        ClientConfig config = ClientConfigTest.testConfig(client.tcp().localAddress().getPort(), 0);
        Assertions.assertThrows(ExecutionException.class, () -> asyncLaunchClient(config));
        client.close();
    }

    @ParameterizedTest
    @ArgumentsSource(Socks5CommandTypeProvider.class)
    @Order(3)
    void testHandshake(Socks5CommandType type) {
        InetSocketAddress proxyAddress = new InetSocketAddress(0);
        InetSocketAddress dstAddress = new InetSocketAddress(0);
        Assertions.assertThrows(ExecutionException.class, () -> ClientHandshake.noAuth(group, type, proxyAddress, dstAddress).get(10, TimeUnit.SECONDS));
    }

    public static Client.Instance asyncLaunchClient(ClientConfig config) throws InterruptedException, ExecutionException {
        CompletableFuture<Client.Instance> promise = new CompletableFuture<>();
        SERVICE.submit(() -> Client.launch(config, promise));
        return promise.get();
    }

    private static class Socks5CommandTypeProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            Socks5CommandType[] types = {Socks5CommandType.CONNECT, Socks5CommandType.BIND};
            return Arrays.stream(types).map(Arguments::of);
        }
    }
}
