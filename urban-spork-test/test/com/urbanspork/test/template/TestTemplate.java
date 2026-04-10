package com.urbanspork.test.template;

import com.urbanspork.client.Client;
import com.urbanspork.common.Runtime;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ClientConfigTest;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.server.Server;
import com.urbanspork.test.server.tcp.DohTestServer;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.ToIntFunction;

abstract class TestTemplate {
    protected static final Logger logger = LoggerFactory.getLogger(TestTemplate.class);
    protected static final ExecutorService POOL = Executors.newThreadPerTaskExecutor(Thread.ofPlatform().name("testTemplatePool-", 1).factory());
    protected static final Runtime RUNTIME = new Runtime();
    protected static final int SERVER_PORT = getPortOrDefault("com.urbanspork.test.server.port", Integer::parseInt);
    protected static final int CLIENT_PORT = getPortOrDefault("com.urbanspork.test.client.port", Integer::parseInt);
    private static ServerSocketChannel DOH_TEST_SERVER;

    protected static FutureInstance<Client.Instance> launchClient(ClientConfig config) throws InterruptedException, ExecutionException {
        CompletableFuture<Client.Instance> promise = new CompletableFuture<>();
        Future<?> task = POOL.submit(() -> Client.launch(config, promise, RUNTIME));
        FutureInstance<Client.Instance> result = new FutureInstance<>(task, promise.get());
        logger.info("new client: {}", result);
        return result;
    }

    protected static FutureInstance<List<Server.Instance>> launchServer(List<ServerConfig> configs) throws InterruptedException, ExecutionException {
        CompletableFuture<List<Server.Instance>> promise = new CompletableFuture<>();
        Future<?> task = POOL.submit(() -> Server.launch(configs, promise, RUNTIME));
        FutureInstance<List<Server.Instance>> result = new FutureInstance<>(task, promise.get());
        logger.info("new server: {}", result);
        return result;
    }

    protected static void close(FutureInstance<Client.Instance> client, FutureInstance<List<Server.Instance>> servers) throws ExecutionException, InterruptedException {
        closeClient(client);
        closeServer(servers);
    }

    protected static void closeClient(FutureInstance<Client.Instance> client) {
        logger.info("client close: {}", client);
        client.instance().close();
        try {
            client.future().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("client close failed", e);
        }
    }

    protected static void closeServer(FutureInstance<List<Server.Instance>> servers) {
        logger.info("server close: {}", servers);
        for (Server.Instance server : servers.instance()) {
            server.close();
        }
        try {
            servers.future().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("server close failed", e);
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

    protected static void updateConfig(ServerConfig config, Parameter parameter) {
        config.setProtocol(parameter.protocol());
        config.setCipher(parameter.cipher());
        config.setPassword(parameter.serverPassword());
        config.setSsl(parameter.sslSetting());
        config.setWs(parameter.wsSetting());
    }
}
