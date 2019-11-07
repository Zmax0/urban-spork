package com.urbanspork.client.gui.console.component;

import java.awt.TrayIcon.MessageType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbanspork.client.Client;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;

public class Proxy {

    private static final Logger logger = LoggerFactory.getLogger(Proxy.class);

    private static final ClientConfig config = Resource.config();

    private static Thread launcher;

    public static void relaunch() {
        if (launcher != null) {
            launcher.interrupt();
        }
        launch();
    }

    public static void launch() {
        ServerConfig currentConfig = config.getCurrent();
        if (currentConfig != null) {
            launcher = new Thread(() -> {
                try {
                    Client.launch(config);
                } catch (InterruptedException e) {
                    Thread thread = Thread.currentThread();
                    logger.info("[{}-{}] was interrupted by relaunch", thread.getName(), thread.getId());
                } catch (Exception e) {
                    logger.error("Launching proxy client launching error", e);
                    String message = e.getMessage();
                    Tray.displayMessage("Error", message, MessageType.ERROR);
                    Tray.setToolTip(message);
                    launcher = null;
                }
            });
            String message = currentConfig.toString();
            Tray.displayMessage("Proxy is running", message, MessageType.INFO);
            Tray.setToolTip(message);
            launcher.setName("Client-Launcher");
            launcher.setDaemon(true);
            launcher.start();
            logger.debug("[{}-{}] launched", launcher.getName(), launcher.getId());
        } else {
            Tray.displayMessage("Proxy is not running", "Please set up a proxy server first", MessageType.INFO);
        }
    }

}
