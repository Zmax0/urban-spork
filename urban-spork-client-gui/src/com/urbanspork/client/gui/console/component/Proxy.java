package com.urbanspork.client.gui.console.component;

import com.urbanspork.client.Client;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.TrayIcon.MessageType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class Proxy {

    private static final Logger logger = LoggerFactory.getLogger(Proxy.class);

    private static final ClientConfig config = Resource.config();

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(new ProxyThreadFactory(config));

    private Proxy() {

    }

    public static void launch() {
        ServerConfig currentConfig = config.getCurrent();
        if (currentConfig != null) {
            executor.submit(() -> {
                try {
                    Client.launch(config);
                } catch (Exception e) {
                    logger.error("Launching proxy client launching error", e);
                    String message = e.getMessage();
                    Tray.displayMessage("Error", message, MessageType.ERROR);
                    Tray.setToolTip(message);
                }
            });
            String message = currentConfig.toString();
            Tray.displayMessage("Proxy is running", message, MessageType.INFO);
            Tray.setToolTip(message);
        } else {
            Tray.displayMessage("Proxy is not running", "Please set up a proxy server first", MessageType.INFO);
        }
    }

    public static void exit() {
        executor.shutdownNow();
    }

    private record ProxyThreadFactory(ClientConfig config) implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("UrbanSporkClient-localhost:" + config.getPort());
            return thread;
        }
    }

}
