package com.urbanspork.client.gui.tray.menu.item;

import java.awt.MenuItem;
import java.awt.event.ActionListener;

import com.urbanspork.client.gui.console.component.Tray;
import com.urbanspork.client.gui.i18n.I18n;

import javafx.application.Platform;

public class ExitMenuItem implements TrayMenuItemBuilder {

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
            Tray.exit();
        };
    }

}