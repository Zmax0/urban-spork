package com.urbanspork.client.mvc.component.tray.menu.item;

import java.awt.MenuItem;
import java.awt.SystemTray;
import java.awt.TrayIcon;

import com.urbanspork.client.mvc.i18n.I18n;

import javafx.application.Platform;

public class ExistMenuItem implements TrayMenuItem {

    private SystemTray tray;

    private TrayIcon trayIcon;

    public ExistMenuItem(SystemTray tray, TrayIcon trayIcon) {
        this.tray = tray;
        this.trayIcon = trayIcon;
    }

    @Override
    public MenuItem getDirectly() {
        return null;
    }

    @Override
    public String getLabel() {
        return I18n.TRAY_EXIT;
    }

    @Override
    public void act() {
        Platform.exit();
        tray.remove(trayIcon);
        System.exit(0);
    }

}