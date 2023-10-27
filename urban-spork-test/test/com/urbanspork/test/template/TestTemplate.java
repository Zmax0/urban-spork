package com.urbanspork.test.template;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.server.Server;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class TestTemplate {

    protected static void launchClient(ExecutorService service, EventExecutor executor, ClientConfig config) throws InterruptedException, ExecutionException {
        Promise<ServerSocketChannel> promise = new DefaultPromise<>(executor);
        service.submit(() -> Client.launch(config, promise));
        Assertions.assertEquals(config.getPort(), promise.await().get().localAddress().getPort());
    }

    protected static void launchServer(ExecutorService service, EventExecutor executor, List<ServerConfig> configs) throws InterruptedException, ExecutionException {
        Promise<List<ServerSocketChannel>> promise = new DefaultPromise<>(executor);
        service.submit(() -> Server.launch(configs, promise));
        Assertions.assertEquals(configs.get(0).getPort(), promise.await().get().get(0).localAddress().getPort());
    }
}