package com.urbanspork.client.gui.tray.menu.item;

import com.urbanspork.client.gui.console.component.Proxy;
import com.urbanspork.client.gui.console.component.Tray;
import com.urbanspork.client.gui.i18n.I18N;
import javafx.application.Platform;

import java.awt.event.ActionListener;

public class ExitMenuItem implements TrayMenuItemBuilder {
    @Override
    public String getLabel() {
        return I18N.getString(I18N.TRAY_EXIT);
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