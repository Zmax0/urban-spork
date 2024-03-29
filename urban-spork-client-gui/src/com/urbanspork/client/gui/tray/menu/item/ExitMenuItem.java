package com.urbanspork.client.gui.tray.menu.item;

import com.urbanspork.client.gui.console.component.Proxy;
import com.urbanspork.client.gui.console.component.Tray;
import com.urbanspork.client.gui.i18n.I18n;
import javafx.application.Platform;

import java.awt.*;
import java.awt.event.ActionListener;

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
            Proxy.exit();
        };
    }

}