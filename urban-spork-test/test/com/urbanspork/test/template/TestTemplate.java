package com.urbanspork.test.template;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.server.Server;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class TestTemplate {

    protected static Future<?> launchClient(ExecutorService service, EventExecutor executor, ClientConfig config) throws InterruptedException, ExecutionException {
        Promise<ServerSocketChannel> promise = executor.newPromise();
        Future<?> future = service.submit(() -> Client.launch(config, promise));
        Assertions.assertEquals(config.getPort(), promise.await().get().localAddress().getPort());
        return future;
    }

    protected static Future<?> launchServer(ExecutorService service, EventExecutor executor, List<ServerConfig> configs) throws InterruptedException, ExecutionException {
        Promise<List<ServerSocketChannel>> promise = executor.newPromise();
        Future<?> future = service.submit(() -> Server.launch(configs, promise));
        Assertions.assertEquals(configs.getFirst().getPort(), promise.await().get().getFirst().localAddress().getPort());
        return future;
    }

    protected static void cancel(Future<?> client, Future<?> server) {
        server.cancel(true);
        client.cancel(true);
        boolean cancel;
        do {
            cancel = server.isCancelled() && client.isCancelled();
        } while (!cancel);
    }
}
