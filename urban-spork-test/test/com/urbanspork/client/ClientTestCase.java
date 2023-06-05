package com.urbanspork.client;

import com.urbanspork.common.protocol.socks.Handshake;
import com.urbanspork.test.TestUtil;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

@DisplayName("Client")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientTestCase {

    private final int[] ports = TestUtil.freePorts(3);
    private final ExecutorService service = Executors.newFixedThreadPool(1);

    @BeforeAll
    void init() {
        TestUtil.testConfig(ports[0], ports[1]).save();
    }

    @Test
    @Order(1)
    void launchClient() {
        Assertions.assertThrows(TimeoutException.class, () -> service.submit(
            () -> Client.main(null)).get(2, TimeUnit.SECONDS)
        );
    }

    @Test
    @Order(2)
    void testHandshake() throws InterruptedException, ExecutionException, TimeoutException {
        InetSocketAddress proxyAddress = new InetSocketAddress(ports[0]);
        InetSocketAddress dstAddress = new InetSocketAddress("localhost", ports[2]);
        Handshake.Result result = Handshake.noAuth(Socks5CommandType.CONNECT, proxyAddress, dstAddress).get(2, TimeUnit.SECONDS);
        Assertions.assertNotEquals(Socks5CommandStatus.SUCCESS, result.response().status());
    }

    @AfterAll
    void shutdown() {
        service.shutdownNow();
    }

}
