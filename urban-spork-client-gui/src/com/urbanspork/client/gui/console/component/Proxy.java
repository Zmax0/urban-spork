package com.urbanspork.client.gui.console.component;

import com.urbanspork.client.Client;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.TrayIcon.MessageType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Proxy {

    private static final Logger logger = LoggerFactory.getLogger(Proxy.class);

    private static final ClientConfig config = Resource.config();

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());

    private static Future<?> proxyingTask;

    private Proxy() {}

    public static void launch() {
        ServerConfig current = config.getCurrent();
        if (current == null) {
            Tray.displayMessage("Proxy is not running", "Please set up a proxy server first", MessageType.INFO);
            return;
        }
        if (proxyingTask != null && !proxyingTask.isCancelled()) {
            proxyingTask.cancel(true);
        }
        proxyingTask = executor.submit(() -> {
            try {
                DefaultPromise<ServerSocketChannel> promise = new DefaultPromise<>() {};
                Client.launch(config, promise);
                promise.await();
            } catch (InterruptedException e) {
                logger.error("Launching proxy client launching error", e);
                String message = e.getMessage();
                Tray.displayMessage("Error", message, MessageType.ERROR);
                Tray.setToolTip(message);
                Thread.currentThread().interrupt();
            }
        });
        String message = current.toString();
        Tray.displayMessage("Proxy is running", message, MessageType.INFO);
        Tray.setToolTip(message);
    }

    public static void exit() {
        executor.shutdown();
    }
}
