package com.urbanspork.client.gui.console.component;

import java.awt.AWTException;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;

import javax.swing.ImageIcon;

import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.i18n.I18n;
import com.urbanspork.client.gui.tray.menu.item.ConsoleMenuItem;
import com.urbanspork.client.gui.tray.menu.item.ExitMenuItem;
import com.urbanspork.client.gui.tray.menu.item.LanguageMenuItem;
import com.urbanspork.client.gui.tray.menu.item.ServersMenuItem;

public class Tray {

    private static final boolean isSupported = SystemTray.isSupported();

    private static final PopupMenu menu = new PopupMenu();

    private static final ImageIcon icon = new ImageIcon(Resource.TRAY_ICON);

    private static final TrayIcon trayIcon = isSupported ? new TrayIcon(icon.getImage(), I18n.PROGRAM_TITLE, menu) : null;

    public static void launch(String[] args) {
        start();
    }

    public static void displayMessage(String caption, String text, MessageType messageType) {
        if (isSupported) {
            trayIcon.displayMessage(caption, text, messageType);
        }
    }

    public static void setToolTip(String tooltip) {
        if (isSupported) {
            StringBuilder builder = new StringBuilder(I18n.TRAY_TOOLTIP);
            builder.append(System.lineSeparator()).append(tooltip);
            trayIcon.setToolTip(builder.toString());
        }
    }

    public static void refresh() {
        menu.remove(0);
        menu.insert(new ServersMenuItem().build(), 0);
    }

    private static void start() {
        if (isSupported) {
            // ==============================
            // tray icon
            // ==============================
            SystemTray tray = SystemTray.getSystemTray();
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                displayMessage("Error", e.getMessage(), MessageType.ERROR);
            }
            // ==============================
            // tray menu
            // ==============================
            menu.add(new ServersMenuItem().build());
            menu.addSeparator();
            menu.add(new ConsoleMenuItem().build());
            menu.addSeparator();
            menu.add(new LanguageMenuItem().build());
            menu.addSeparator();
            menu.add(new ExitMenuItem().build());
        }
    }

}