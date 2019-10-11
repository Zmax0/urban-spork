package com.urbanspork.client.mvc.component;

import java.awt.AWTException;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;

import javax.swing.ImageIcon;

import com.urbanspork.client.mvc.Resource;
import com.urbanspork.client.mvc.component.tray.menu.item.ConsoleMenuItem;
import com.urbanspork.client.mvc.component.tray.menu.item.ExistMenuItem;
import com.urbanspork.client.mvc.component.tray.menu.item.LanguageMenuItem;
import com.urbanspork.client.mvc.component.tray.menu.item.ServersMenuItem;
import com.urbanspork.client.mvc.i18n.I18n;

public class Tray {

    private static final boolean isSupported = SystemTray.isSupported();

    private final PopupMenu menu = new PopupMenu();

    private TrayIcon trayIcon;

    public final void displayMessage(String caption, String text, MessageType messageType) {
        if (isSupported && trayIcon != null) {
            trayIcon.displayMessage(caption, text, messageType);
        }
    }

    public final void setToolTip(String tooltip) {
        if (isSupported && trayIcon != null) {
            StringBuilder builder = new StringBuilder(I18n.TRAY_TOOLTIP);
            builder.append(System.lineSeparator()).append(tooltip);
            trayIcon.setToolTip(builder.toString());
        }
    }

    public final void refresh() {
        menu.remove(0);
        menu.insert(new ServersMenuItem().get(), 0);
    }

    public void start(String[] args) {
        if (isSupported) {
            // ==============================
            // tray icon
            // ==============================
            SystemTray tray = SystemTray.getSystemTray();
            ImageIcon icon = new ImageIcon(Resource.TRAY_ICON);
            trayIcon = new TrayIcon(icon.getImage(), I18n.PROGRAM_TITLE, menu);
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                displayMessage("Error", e.getMessage(), MessageType.ERROR);
            }
            // ==============================
            // tray menu
            // ==============================
            menu.add(new ServersMenuItem().get());
            menu.addSeparator();
            menu.add(new ConsoleMenuItem().get());
            menu.addSeparator();
            menu.add(new LanguageMenuItem().get());
            menu.addSeparator();
            menu.add(new ExistMenuItem(tray, trayIcon).get());
        }
    }

}