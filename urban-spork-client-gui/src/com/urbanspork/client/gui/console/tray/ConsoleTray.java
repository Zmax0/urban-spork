package com.urbanspork.client.gui.console.tray;

import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.Console;
import com.urbanspork.client.gui.console.tray.menu.item.ConsoleMenuItem;
import com.urbanspork.client.gui.console.tray.menu.item.ExitMenuItem;
import com.urbanspork.client.gui.console.tray.menu.item.LanguageMenuItem;
import com.urbanspork.client.gui.console.tray.menu.item.ServersMenuItem;
import com.urbanspork.client.gui.i18n.I18N;
import com.urbanspork.client.gui.tray.Tray;
import com.urbanspork.client.gui.tray.TrayIcon;
import javafx.application.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.TrayIcon.MessageType;

public final class ConsoleTray implements Tray {

    private final JPopupMenu menu = new JPopupMenu();
    private final TrayIcon trayIcon;
    private final Console console;

    public ConsoleTray(Console console) {
        this.console = console;
        this.trayIcon = new TrayIcon(new ImageIcon(Resource.TRAY_ICON).getImage(), I18N.getString(I18N.PROGRAM_TITLE), () -> Platform.runLater(console::show), menu);
        start();
    }

    @Override
    public void displayMessage(String caption, String text, MessageType messageType) {
        trayIcon.displayMessage(caption, text, messageType);
    }

    @Override
    public void setToolTip(String tooltip) {
        trayIcon.setToolTip(I18N.getString(I18N.TRAY_TOOLTIP) + System.lineSeparator() + tooltip);
    }

    @Override
    public void refresh() {
        menu.remove(0);
        menu.insert(new ServersMenuItem(console, this).build(), 0);
    }

    @Override
    public void exit() {
        SystemTray.getSystemTray().remove(trayIcon);
    }

    private void start() {
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
        menu.add(new ServersMenuItem(console, this).build());
        menu.addSeparator();
        menu.add(new ConsoleMenuItem(console).build());
        menu.addSeparator();
        menu.add(new LanguageMenuItem(this).build());
        menu.addSeparator();
        menu.add(new ExitMenuItem().build());
    }
}