package com.urbanspork.test.template;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.server.Server;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

abstract class TestTemplate {
    protected static final ExecutorService POOL = Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory());

    protected static Client.Instance launchClient(ClientConfig config)
        throws InterruptedException, ExecutionException {
        CompletableFuture<Client.Instance> promise = new CompletableFuture<>();
        POOL.submit(() -> Client.launch(config, promise));
        return promise.get();
    }

    protected static List<Server.Instance> launchServer(List<ServerConfig> configs)
        throws InterruptedException, ExecutionException {
        CompletableFuture<List<Server.Instance>> promise = new CompletableFuture<>();
        POOL.submit(() -> Server.launch(configs, promise));
        return promise.get();
    }

    protected static void closeServer(List<Server.Instance> servers) {
        for (Server.Instance server : servers) {
            server.close();
        }
    }
}
