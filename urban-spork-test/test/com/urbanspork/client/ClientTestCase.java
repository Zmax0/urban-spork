package com.urbanspork.client;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTestCase;
import com.urbanspork.common.protocol.socks.Handshake;
import com.urbanspork.test.TestUtil;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.util.concurrent.DefaultPromise;
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

    private final int[] ports = TestUtil.freePorts(2);

    @RepeatedTest(2)
    @Order(1)
    void launchClient() throws InterruptedException {
        launchClient(ClientConfigTestCase.testConfig(ports));
    }

    @ParameterizedTest
    @ArgumentsSource(Socks5CommandTypeProvider.class)
    @Order(2)
    void testHandshake(Socks5CommandType type) throws InterruptedException, ExecutionException, TimeoutException {
        InetSocketAddress proxyAddress = new InetSocketAddress(ports[0]);
        InetSocketAddress dstAddress = new InetSocketAddress(ports[1]);
        Handshake.Result result = Handshake.noAuth(type, proxyAddress, dstAddress).get(10, TimeUnit.SECONDS);
        Assertions.assertNotEquals(Socks5CommandStatus.SUCCESS, result.response().status());
    }

    public static Future<?> launchClient(ClientConfig config) throws InterruptedException {
        DefaultEventLoop executor = new DefaultEventLoop();
        Promise<ServerSocketChannel> promise = new DefaultPromise<>(executor);
        Future<?> future = Executors.newFixedThreadPool(1).submit(() -> Client.launch(config, promise));
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