package com.urbanspork.client.mvc.tray.menu.item;

import java.awt.MenuItem;
import java.awt.event.ActionListener;

import com.urbanspork.client.mvc.i18n.I18n;

import javafx.application.Platform;

public class ExistMenuItem implements TrayMenuItemBuilder {

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
            System.exit(0);
        };
    }

}