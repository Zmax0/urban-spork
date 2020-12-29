package com.urbanspork.client.gui.tray.menu.item;

import java.awt.MenuItem;
import java.awt.event.ActionListener;
import java.util.Optional;

public interface TrayMenuItemBuilder {

    MenuItem getMenuItem();

    String getLabel();

    ActionListener getActionListener();

    default MenuItem build() {
        return Optional.ofNullable(getMenuItem()).orElse(build(getLabel(), getActionListener()));
    }

    default MenuItem build(String label, ActionListener listener) {
        MenuItem item = new MenuItem();
        item.setLabel(label);
        item.addActionListener(listener);
        return item;
    }

}
