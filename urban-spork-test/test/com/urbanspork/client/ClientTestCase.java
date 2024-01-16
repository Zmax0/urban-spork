package com.urbanspork.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTestCase;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.protocol.socks.ClientHandshake;
import com.urbanspork.test.TestUtil;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.Stream;

@DisplayName("Client")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientTestCase {
    private static final ExecutorService SERVICE = Executors.newVirtualThreadPerTaskExecutor();
    private final int[] ports = TestUtil.freePorts(2);
    private final EventLoopGroup group = new NioEventLoopGroup();

    @Test
    @Order(1)
    void testExit() {
        ClientConfig config = ClientConfigTestCase.testConfig(ports);
        ConfigHandler.DEFAULT.save(config);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
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
    void asyncLaunchClient() throws InterruptedException, ExecutionException, TimeoutException {
        Future<?> f1 = asyncLaunchClient(ClientConfigTestCase.testConfig(ports));
        Future<?> f2 = asyncLaunchClient(ClientConfigTestCase.testConfig(ports));
        f2.get(5, TimeUnit.SECONDS);
        Assertions.assertFalse(f1.isDone());
        Assertions.assertTrue(f2.isDone());
        f1.cancel(true);
    }

    @ParameterizedTest
    @ArgumentsSource(Socks5CommandTypeProvider.class)
    @Order(3)
    void testHandshake(Socks5CommandType type) {
        InetSocketAddress proxyAddress = new InetSocketAddress(ports[0]);
        InetSocketAddress dstAddress = new InetSocketAddress(ports[1]);
        Assertions.assertThrows(ExecutionException.class, () -> ClientHandshake.noAuth(group, type, proxyAddress, dstAddress).get(10, TimeUnit.SECONDS));
    }

    public static Future<?> asyncLaunchClient(ClientConfig config) throws InterruptedException {
        DefaultEventLoop executor = new DefaultEventLoop();
        Promise<ServerSocketChannel> promise = executor.newPromise();
        Future<?> future = SERVICE.submit(() -> Client.launch(config, promise));
        promise.await();
        executor.shutdownGracefully();
        return future;
    }

    private static class Socks5CommandTypeProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            Socks5CommandType[] types = {Socks5CommandType.CONNECT, Socks5CommandType.BIND};
            return Arrays.stream(types).map(Arguments::of);
        }
    }
}
