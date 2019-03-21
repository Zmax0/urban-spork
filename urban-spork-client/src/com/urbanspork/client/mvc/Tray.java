package com.urbanspork.client.mvc;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;

import javax.swing.ImageIcon;

import javafx.application.Platform;

public class Tray {

    private static final boolean IS_SUPPORTED = SystemTray.isSupported();

    private static TrayIcon TRAY_ICON;

    public static void launch(String[] args) throws Exception {
        if (IS_SUPPORTED) {
            PopupMenu menu = new PopupMenu();
            MenuItem item0 = new MenuItem("Console");
            item0.addActionListener(listener -> {
                Platform.runLater(() -> {
                    Console.show();
                });
            });
            MenuItem item1 = new MenuItem("Exit");
            item1.addActionListener(listener -> {
                System.exit(0);
            });
            menu.add(item0);
            menu.addSeparator();
            menu.add(item1);

            ClassLoader classLoader = Tray.class.getClassLoader();
            SystemTray tray = SystemTray.getSystemTray();
            ImageIcon icon = new ImageIcon(classLoader.getResource("com/urbanspork/client/mvc/resource/icon16x16.png"));
            TRAY_ICON = new TrayIcon(icon.getImage(), "Proxy Client", menu);
            TRAY_ICON.setImageAutoSize(true);
            tray.add(TRAY_ICON);
        }
    }

    public static void displayMessage(String caption, String text, MessageType messageType) {
        if (IS_SUPPORTED) {
            TRAY_ICON.displayMessage(caption, text, messageType);
        }
    }

}
