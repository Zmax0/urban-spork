package com.urbanspork.test;

import com.urbanspork.client.Client;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfigTestCase;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestUtil {

    private static final Logger logger = LoggerFactory.getLogger(TestUtil.class);

    private TestUtil() {}

    public static int[] freePorts(int size) {
        int[] ports = new int[size];
        List<ServerSocket> sockets = new ArrayList<>(size);
        try {
            for (int i = 0; i < size; i++) {
                ServerSocket socket;
                try {
                    socket = new ServerSocket(0);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                ports[i] = socket.getLocalPort();
                sockets.add(socket);
            }
        } finally {
            for (ServerSocket socket : sockets) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.error("Close socket error", e);
                }
            }
        }
        return ports;
    }

    public static ClientConfig testConfig(int clientPort, int serverPort) {
        ClientConfig config = new ClientConfig();
        config.setPort(clientPort);
        config.setIndex(0);
        config.setServers(List.of(ServerConfigTestCase.testConfig(serverPort)));
        return config;
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
