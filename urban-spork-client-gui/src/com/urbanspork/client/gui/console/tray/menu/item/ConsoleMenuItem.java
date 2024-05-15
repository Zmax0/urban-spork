package com.urbanspork.client.gui.console.tray.menu.item;

import com.urbanspork.client.gui.console.Console;
import com.urbanspork.client.gui.i18n.I18N;
import javafx.application.Platform;

import java.awt.event.ActionListener;

public class ConsoleMenuItem implements TrayMenuItemBuilder {

    private final Console console;

    public ConsoleMenuItem(Console console) {
        this.console = console;
    }

    @Override
    public String getLabel() {
        return I18N.getString(I18N.TRAY_MENU_CONSOLE);
    }

    @Override
    public ActionListener getActionListener() {
        return e -> Platform.runLater(console::show);
    }
}