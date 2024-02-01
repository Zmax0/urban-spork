package com.urbanspork.test.template;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.server.Server;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestTemplate {

    protected static final ExecutorService POOL = Executors.newVirtualThreadPerTaskExecutor();

    protected static Map.Entry<ServerSocketChannel, DatagramChannel> launchClient(ClientConfig config)
        throws InterruptedException, ExecutionException {
        CompletableFuture<Map.Entry<ServerSocketChannel, DatagramChannel>> promise = new CompletableFuture<>();
        POOL.submit(() -> Client.launch(config, promise));
        return promise.get();
    }

    protected static List<Map.Entry<ServerSocketChannel, Optional<DatagramChannel>>> launchServer(List<ServerConfig> configs)
        throws InterruptedException, ExecutionException {
        CompletableFuture<List<Map.Entry<ServerSocketChannel, Optional<DatagramChannel>>>> promise = new CompletableFuture<>();
        POOL.submit(() -> Server.launch(configs, promise));
        return promise.get();
    }
}
