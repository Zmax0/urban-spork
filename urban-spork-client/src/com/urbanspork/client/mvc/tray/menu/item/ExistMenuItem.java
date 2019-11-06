package com.urbanspork.client.mvc.tray.menu.item;

import java.awt.MenuItem;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionListener;

import com.urbanspork.client.mvc.i18n.I18n;

import javafx.application.Platform;

public class ExistMenuItem implements TrayMenuItemBuilder {

    private SystemTray tray;

    private TrayIcon trayIcon;

    public ExistMenuItem(SystemTray tray, TrayIcon trayIcon) {
        this.tray = tray;
        this.trayIcon = trayIcon;
    }

    @Override
    public MenuItem getMenuItem() {
        return null;
    }

    @Override
    public String getLabel() {
        return I18n.TRAY_EXIT;
    }

    @Override
    public ActionListener getActionListener() {
        return e -> {
            Platform.exit();
            tray.remove(trayIcon);
            System.exit(0);
        };
    }

}