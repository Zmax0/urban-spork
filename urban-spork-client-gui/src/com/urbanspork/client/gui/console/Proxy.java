package com.urbanspork.client.gui.console;

import com.urbanspork.client.Client;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.tray.Tray;
import com.urbanspork.common.Runtime;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;

import java.awt.TrayIcon.MessageType;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

class Proxy {
    private static final ClientConfig config = Resource.config();
    private final Runtime runtime = new Runtime();
    private final Tray tray;
    private Client.Instance client;

    Proxy(Tray tray) {
        this.tray = tray;
    }

    Optional<Client.Instance> launch() {
        ServerConfig current = config.getCurrent();
        if (current == null) {
            tray.displayMessage("Proxy is not running", "Please set up a proxy server first", MessageType.INFO);
            return Optional.empty();
        }
        try {
            client = launch0();
            String message = current.toString();
            tray.displayMessage("Proxy is running", message, MessageType.INFO);
            tray.setToolTip(message);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            String message = e.getMessage();
            tray.displayMessage("Error", message, MessageType.ERROR);
            tray.setToolTip(message);
        }
        return Optional.ofNullable(client);
    }

    private Client.Instance launch0() throws ExecutionException, InterruptedException {
        if (client == null) {
            return Client.launch(runtime, Proxy.config).get();
        } else if (client.tcp().localAddress().getPort() != Proxy.config.getPort()) {
            client.close();
            return Client.launch(runtime, Proxy.config).get();
        } else {
            return Client.reload(runtime, client, Proxy.config.getCurrent()).get();
        }
    }

    void exit() {
        Optional.ofNullable(client).ifPresent(Client.Instance::close);
        runtime.close();
    }
}
