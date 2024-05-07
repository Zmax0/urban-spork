package com.urbanspork.client.gui.console;

import com.urbanspork.client.Client;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.tray.Tray;
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
    private final Tray tray;
    private Client.Instance client;

    public Proxy(Tray tray) {
        this.tray = tray;
    }

    public void launch() {
        ServerConfig current = config.getCurrent();
        if (current == null) {
            tray.displayMessage("Proxy is not running", "Please set up a proxy server first", MessageType.INFO);
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
            tray.displayMessage("Proxy is running", message, MessageType.INFO);
            tray.setToolTip(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            String message = e.getMessage();
            tray.displayMessage("Error", message, MessageType.ERROR);
            tray.setToolTip(message);
        }
    }

    public void exit() {
        client.close();
        executor.shutdown();
    }
}
