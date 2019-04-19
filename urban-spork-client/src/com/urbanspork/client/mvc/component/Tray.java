package com.urbanspork.client.mvc.component;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;

import javax.swing.ImageIcon;

import com.urbanspork.client.mvc.Component;
import com.urbanspork.client.mvc.Resource;

import javafx.application.Platform;

public class Tray {

    private static final boolean IS_SUPPORTED = SystemTray.isSupported();

    private TrayIcon trayIcon;

    public void launch(String[] args) throws Exception {
        if (IS_SUPPORTED) {
            PopupMenu menu = new PopupMenu();
            MenuItem item0 = new MenuItem("Console");
            item0.addActionListener(listener -> {
                Platform.runLater(() -> {
                    Console console = Component.Console.get();
                    console.show();
                });
            });
            MenuItem item1 = new MenuItem("Exit");
            item1.addActionListener(listener -> {
                System.exit(0);
            });
            menu.add(item0);
            menu.addSeparator();
            menu.add(item1);

            SystemTray tray = SystemTray.getSystemTray();
            ImageIcon icon = new ImageIcon(Resource.TRAY_ICON.getAbsolutePath());
            trayIcon = new TrayIcon(icon.getImage(), "Proxy Client", menu);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        }
    }

    public void displayMessage(String caption, String text, MessageType messageType) {
        if (IS_SUPPORTED) {
            trayIcon.displayMessage(caption, text, messageType);
        }
    }

}
