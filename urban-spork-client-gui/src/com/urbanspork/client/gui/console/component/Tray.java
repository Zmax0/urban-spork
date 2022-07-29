package com.urbanspork.client.gui.console.component;

import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.i18n.I18n;
import com.urbanspork.client.gui.tray.menu.item.ConsoleMenuItem;
import com.urbanspork.client.gui.tray.menu.item.ExitMenuItem;
import com.urbanspork.client.gui.tray.menu.item.LanguageMenuItem;
import com.urbanspork.client.gui.tray.menu.item.ServersMenuItem;

import javax.swing.*;
import java.awt.*;
import java.awt.TrayIcon.MessageType;

public class Tray {

    private static final boolean IS_SUPPORTED = SystemTray.isSupported();

    private static final PopupMenu menu = new PopupMenu();

    private static final ImageIcon icon = new ImageIcon(Resource.TRAY_ICON);

    private static final TrayIcon trayIcon = IS_SUPPORTED ? new TrayIcon(icon.getImage(), I18n.PROGRAM_TITLE, menu) : null;

    private static Console console;

    private Tray() {

    }

    public static void init(Console console) {
        Tray.console = console;
        start();
    }

    public static void displayMessage(String caption, String text, MessageType messageType) {
        if (IS_SUPPORTED) {
            trayIcon.displayMessage(caption, text, messageType);
        }
    }

    public static void setToolTip(String tooltip) {
        if (IS_SUPPORTED) {
            trayIcon.setToolTip(I18n.TRAY_TOOLTIP + System.lineSeparator() + tooltip);
        }
    }

    public static void refresh() {
        menu.remove(0);
        menu.insert(new ServersMenuItem(console).build(), 0);
    }

    public static void exit() {
        SystemTray.getSystemTray().remove(trayIcon);
    }

    private static void start() {
        if (IS_SUPPORTED) {
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
            menu.add(new ServersMenuItem(console).build());
            menu.addSeparator();
            menu.add(new ConsoleMenuItem(console).build());
            menu.addSeparator();
            menu.add(new LanguageMenuItem().build());
            menu.addSeparator();
            menu.add(new ExitMenuItem().build());
        }
    }

}