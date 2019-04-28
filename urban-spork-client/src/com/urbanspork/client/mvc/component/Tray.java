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

    private static final boolean IS_SUPPORTED = SystemTray.isSupported();

    private TrayIcon trayIcon;

    public void launch(String[] args) throws Exception {
        if (IS_SUPPORTED) {
            PopupMenu menu = new PopupMenu();
            MenuItem item0 = new MenuItem(I18n.TRAY_MENU_CONSOLE);
            item0.addActionListener(l -> {
                Platform.runLater(() -> {
                    Console console = Component.Console.get();
                    console.show();
                });
            });
            MenuItem item1 = new MenuItem(I18n.TRAY_EXIT);
            item1.addActionListener(l -> {
                System.exit(0);
            });
            menu.add(item0);
            menu.addSeparator();
            menu.add(item1);

            SystemTray tray = SystemTray.getSystemTray();
            ImageIcon icon = new ImageIcon(Resource.TRAY_ICON);
            trayIcon = new TrayIcon(icon.getImage(), I18n.PRAGRAM_TITLE, menu);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        }
    }

    public void displayMessage(String caption, String text, MessageType messageType) {
        if (IS_SUPPORTED) {
            trayIcon.displayMessage(caption, text, messageType);
        }
    }

    public void setToolTip(String tooltip) {
        if (IS_SUPPORTED) {
            StringBuilder builder = new StringBuilder(I18n.TRAY_TOOLTIP);
            builder.append(System.lineSeparator()).append(tooltip);
            trayIcon.setToolTip(builder.toString());
        }
    }

}
