package com.urbanspork.client.mvc.tray.menu.item;

import java.awt.Menu;
import java.awt.event.ActionListener;

import com.urbanspork.client.mvc.console.component.Console;
import com.urbanspork.client.mvc.i18n.I18n;

import javafx.application.Platform;

public class ConsoleMenuItem implements TrayMenuItemBuilder {

    @Override
    public Menu getMenuItem() {
        return null;
    }

    @Override
    public String getLabel() {
        return I18n.TRAY_MENU_CONSOLE;
    }

    @Override
    public ActionListener getActionListener() {
        return e -> {
            Platform.runLater(() -> {
                Console.show();
            });
        };
    }
}