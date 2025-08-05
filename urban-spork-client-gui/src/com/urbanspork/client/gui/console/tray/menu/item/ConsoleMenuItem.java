package com.urbanspork.client.gui.console.tray.menu.item;

import com.urbanspork.client.gui.console.Console;
import com.urbanspork.client.gui.i18n.I18N;
import javafx.application.Platform;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeSupport;

public record ConsoleMenuItem(Console console, PropertyChangeSupport changeSupport) implements TrayMenuItemBuilder {

    @Override
    public String getTextKey() {
        return I18N.TRAY_MENU_CONSOLE;
    }

    @Override
    public ActionListener getActionListener() {
        return _ -> Platform.runLater(console::show);
    }

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        return changeSupport;
    }
}