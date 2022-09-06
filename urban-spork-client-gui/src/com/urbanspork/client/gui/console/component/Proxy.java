package com.urbanspork.client.gui.console.component;

import com.urbanspork.client.Client;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.TrayIcon.MessageType;

public class Proxy {

    private static final Logger logger = LoggerFactory.getLogger(Proxy.class);

    private static final ClientConfig config = Resource.config();

    private static Thread launcher;

    private Proxy() {

    }

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
                    logger.info("Interrupt thread [{}]", thread.getName());
                    thread.interrupt();
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
            launcher.setName("UrbanSporkClient-" + currentConfig.getHost() + ':' + currentConfig.getPort());
            launcher.setDaemon(true);
            launcher.start();
            logger.debug("[{}-{}] launched", launcher.getName(), launcher.getId());
        } else {
            Tray.displayMessage("Proxy is not running", "Please set up a proxy server first", MessageType.INFO);
        }
    }

    public static void exit() {
        launcher.interrupt();
    }

}
