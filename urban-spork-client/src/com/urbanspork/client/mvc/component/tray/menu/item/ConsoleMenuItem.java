package com.urbanspork.client.mvc.component.tray.menu.item;

import java.awt.Menu;

import com.urbanspork.client.mvc.Components;
import com.urbanspork.client.mvc.i18n.I18n;

import javafx.application.Platform;

public class ConsoleMenuItem implements TrayMenuItem {

    @Override
    public Menu getDirectly() {
        return null;
    }

    @Override
    public String getLabel() {
        return I18n.TRAY_MENU_CONSOLE;
    }

    @Override
    public void act() {
        Platform.runLater(() -> {
            Components.Console.show();
        });
    }

}