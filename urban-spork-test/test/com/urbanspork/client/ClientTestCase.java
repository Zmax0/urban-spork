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

import java.net.InetSocketAddress;
import java.util.concurrent.*;

@DisplayName("Client")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientTestCase {

    private final int[] ports = TestUtil.freePorts(2);
    private final ExecutorService service = Executors.newFixedThreadPool(1);

    @Test
    @Order(1)
    void launchClient() {
        ClientConfig config = ClientConfigTestCase.testConfig(ports);
        service.submit(() -> launchClient(config).get(2, TimeUnit.SECONDS));
    }

    @Test
    @Order(2)
    void testHandshake() throws InterruptedException, ExecutionException, TimeoutException {
        InetSocketAddress proxyAddress = new InetSocketAddress(ports[0]);
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", ports[1]);
        Handshake.Result result = Handshake.noAuth(Socks5CommandType.CONNECT, proxyAddress, dstAddress).get(2, TimeUnit.SECONDS);
        Assertions.assertNotEquals(Socks5CommandStatus.SUCCESS, result.response().status());
    }

    @AfterAll
    void shutdown() {
        service.shutdownNow();
    }

    public static Future<?> launchClient(ClientConfig config) throws InterruptedException {
        DefaultEventLoop executor = new DefaultEventLoop();
        Promise<ServerSocketChannel> promise = new DefaultPromise<>(executor);
        Future<?> future = Executors.newFixedThreadPool(1).submit(() -> Client.launch(config, promise));
        promise.await();
        executor.shutdownGracefully();
        return future;
    }
}
