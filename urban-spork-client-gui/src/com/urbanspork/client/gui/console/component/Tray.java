package com.urbanspork.client.gui.console.component;

import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.i18n.I18N;
import com.urbanspork.client.gui.tray.menu.item.ConsoleMenuItem;
import com.urbanspork.client.gui.tray.menu.item.ExitMenuItem;
import com.urbanspork.client.gui.tray.menu.item.LanguageMenuItem;
import com.urbanspork.client.gui.tray.menu.item.ServersMenuItem;

import javax.swing.*;
import java.awt.*;
import java.awt.TrayIcon.MessageType;

import com.urbanspork.client.gui.tray.TrayIcon;
import javafx.application.Platform;

public class Tray {

    private static final boolean IS_SUPPORTED = SystemTray.isSupported();
    private static final JPopupMenu menu = new JPopupMenu();
    private static final ImageIcon icon = new ImageIcon(Resource.TRAY_ICON);
    private static TrayIcon trayIcon;
    private static Console console;

    private Tray() {}

    public static void init(Console console) {
        Tray.console = console;
        if (IS_SUPPORTED) {
            trayIcon = new TrayIcon(icon.getImage(), I18N.getString(I18N.PROGRAM_TITLE), () -> Platform.runLater(console::show), menu);
            start();
        }
    }

    public static void displayMessage(String caption, String text, MessageType messageType) {
        if (IS_SUPPORTED) {
            trayIcon.displayMessage(caption, text, messageType);
        }
    }

    public static void setToolTip(String tooltip) {
        if (IS_SUPPORTED) {
            trayIcon.setToolTip(I18N.getString(I18N.TRAY_TOOLTIP) + System.lineSeparator() + tooltip);
        }
    }

    public static void refresh() {
        menu.remove(0);
        menu.insert(new ServersMenuItem(console).build(), 0);
    }

    public static void exit() {
        if (IS_SUPPORTED) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }

    private static void start() {
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
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
            // ignore
        }
        SwingUtilities.updateComponentTreeUI(menu);
        menu.add(new ServersMenuItem(console).build());
        menu.addSeparator();
        menu.add(new ConsoleMenuItem(console).build());
        menu.addSeparator();
        menu.add(new LanguageMenuItem().build());
        menu.addSeparator();
        menu.add(new ExitMenuItem().build());
    }
}