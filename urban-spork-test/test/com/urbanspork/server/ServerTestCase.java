package com.urbanspork.server;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import com.urbanspork.test.TestDice;
import com.urbanspork.test.TestUtil;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

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
        ServerConfig config = configs.get(0);
        config.setHost("www.urban-spork.com");
        Assertions.assertThrows(IllegalArgumentException.class, () -> Server.launch(configs), "None available server");
    }

    @Test
    void launchFailed() throws InterruptedException {
        int port = TestDice.rollPort();
        List<ServerConfig> configs = ServerConfigTestCase.testConfig(new int[]{port, port});
        DefaultEventLoop executor = new DefaultEventLoop();
        Promise<List<ServerSocketChannel>> promise = new DefaultPromise<>(executor);
        Server.launch(configs, promise);
        promise.await(5, TimeUnit.SECONDS);
        Assertions.assertFalse(promise.isSuccess());
    }

    @Test
    void shutdown() {
        List<ServerConfig> configs = ServerConfigTestCase.testConfig(TestUtil.freePorts(2));
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> future = service.submit(() -> Server.launch(configs));
        try {
            future.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            future.cancel(true);
        }
        Assertions.assertTrue(future.isCancelled());
        service.shutdown();
    }
}
