package com.urbanspork.client.mvc.component;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;

import javax.swing.ImageIcon;

import com.urbanspork.client.mvc.Component;
import com.urbanspork.client.mvc.Resource;
import com.urbanspork.client.mvc.i18n.I18n;

import javafx.application.Platform;

public class Tray {

    private static final boolean isSupported = SystemTray.isSupported();

    private static TrayIcon trayIcon;

    public static void launch(String[] args) throws Exception {
        if (isSupported) {
            // ==============================
            // tray menu
            // ==============================
            PopupMenu menu = new PopupMenu();
            MenuItem item0 = new MenuItem(I18n.TRAY_MENU_CONSOLE);
            MenuItem item1 = new MenuItem(I18n.TRAY_EXIT);
            menu.add(item0);
            menu.addSeparator();
            menu.add(item1);
            // ==============================
            // tray icon
            // ==============================
            SystemTray tray = SystemTray.getSystemTray();
            ImageIcon icon = new ImageIcon(Resource.TRAY_ICON);
            trayIcon = new TrayIcon(icon.getImage(), I18n.PRAGRAM_TITLE, menu);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            // ==============================
            // menu item listener
            // ==============================
            item0.addActionListener(l -> {
                Platform.runLater(() -> {
                    Console console = Component.Console.get();
                    console.show();
                });
            });
            item1.addActionListener(l -> {
                Platform.exit();
                tray.remove(trayIcon);
                System.exit(0);
            });
        }
    }

    public static void displayMessage(String caption, String text, MessageType messageType) {
        if (isSupported && trayIcon != null) {
            trayIcon.displayMessage(caption, text, messageType);
        }
    }

    public static void setToolTip(String tooltip) {
        if (isSupported && trayIcon != null) {
            StringBuilder builder = new StringBuilder(I18n.TRAY_TOOLTIP);
            builder.append(System.lineSeparator()).append(tooltip);
            trayIcon.setToolTip(builder.toString());
        }
    }

}
