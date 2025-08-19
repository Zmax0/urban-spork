package com.urbanspork.test.template;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.tcp.DohTestServer;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.ToIntFunction;

abstract class TestTemplate {
    protected static final ExecutorService POOL = Executors.newVirtualThreadPerTaskExecutor();
    protected static final int SERVER_PORT = getPortOrDefault("com.urbanspork.test.server.port", Integer::parseInt);
    protected static final int CLIENT_PORT = getPortOrDefault("com.urbanspork.test.client.port", Integer::parseInt);
    private static ServerSocketChannel DOH_TEST_SERVER;

    protected static Client.Instance launchClient(ClientConfig config) throws InterruptedException, ExecutionException {
        CompletableFuture<Client.Instance> promise = new CompletableFuture<>();
        POOL.submit(() -> Client.launch(config, promise));
        return promise.get();
    }

    protected static List<Server.Instance> launchServer(List<ServerConfig> configs) throws InterruptedException, ExecutionException {
        CompletableFuture<List<Server.Instance>> promise = new CompletableFuture<>();
        POOL.submit(() -> Server.launch(configs, promise));
        return promise.get();
    }

    protected static void closeServer(List<Server.Instance> servers) {
        for (Server.Instance server : servers) {
            server.close();
        }
    }

    private static int getPortOrDefault(String key, ToIntFunction<String> converter) {
        String property = System.getProperty(key);
        return property == null ? 0 : converter.applyAsInt(property);
    }

    protected static ClientConfig testConfig() {
        return ClientConfigTest.testConfig(CLIENT_PORT, SERVER_PORT);
    }

    protected static ServerSocketChannel dohTestServer() throws ExecutionException, InterruptedException {
        if (DOH_TEST_SERVER == null || !DOH_TEST_SERVER.isActive()) {
            DefaultPromise<ServerSocketChannel> promise = new DefaultPromise<>() {};
            POOL.submit(() -> DohTestServer.launch(0, promise));
            DOH_TEST_SERVER = promise.get();
        }
        return DOH_TEST_SERVER;
    }
}
