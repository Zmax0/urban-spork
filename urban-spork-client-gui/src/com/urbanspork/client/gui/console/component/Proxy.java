package com.urbanspork.client.gui.console.component;

import com.urbanspork.client.Client;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;

import java.awt.TrayIcon.MessageType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Proxy {

    private static final ClientConfig config = Resource.config();

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static Client.Instance client;

    private Proxy() {}

    public static void launch() {
        ServerConfig current = config.getCurrent();
        if (current == null) {
            Tray.displayMessage("Proxy is not running", "Please set up a proxy server first", MessageType.INFO);
            return;
        }
        if (client != null) {
            client.close();
        }
        CompletableFuture<Client.Instance> promise = new CompletableFuture<>();
        executor.submit(() -> Client.launch(config, promise));
        try {
            client = promise.get();
            String message = current.toString();
            Tray.displayMessage("Proxy is running", message, MessageType.INFO);
            Tray.setToolTip(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            String message = e.getMessage();
            Tray.displayMessage("Error", message, MessageType.ERROR);
            Tray.setToolTip(message);
        }
    }

    public static void exit() {
        client.close();
        executor.shutdown();
    }
}
