package com.urbanspork.client.gui.console;

import com.urbanspork.client.gui.console.tray.ConsoleTray;
import com.urbanspork.client.gui.tray.Unsupported;
import javafx.application.Preloader;
import javafx.stage.Stage;

import java.awt.*;

public class ConsolePreloader extends Preloader {
    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        if (info.getType().equals(StateChangeNotification.Type.BEFORE_INIT)
            && info.getApplication() instanceof Console console) {
            console.tray = SystemTray.isSupported() ? new ConsoleTray(console) : new Unsupported();
            console.proxy = new Proxy(console.tray);
            console.proxy.launch().ifPresent(console.instance::set);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // skip
    }
}
