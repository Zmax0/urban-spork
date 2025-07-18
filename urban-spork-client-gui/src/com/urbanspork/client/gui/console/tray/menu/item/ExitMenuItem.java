package com.urbanspork.client.gui.console.tray.menu.item;

import com.urbanspork.client.gui.i18n.I18N;
import javafx.application.Platform;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeSupport;

public class ExitMenuItem implements TrayMenuItemBuilder {
    private final PropertyChangeSupport changeSupport;

    public ExitMenuItem(PropertyChangeSupport changeSupport) {
        this.changeSupport = changeSupport;
    }

    @Override
    public String getTextKey() {
        return I18N.TRAY_EXIT;
    }

    @Override
    public ActionListener getActionListener() {
        return e -> Platform.exit();
    }

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return changeSupport;
    }
}