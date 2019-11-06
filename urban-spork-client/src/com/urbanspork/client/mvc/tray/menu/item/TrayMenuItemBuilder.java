package com.urbanspork.client.mvc.tray.menu.item;

import java.awt.MenuItem;
import java.awt.event.ActionListener;
import java.util.Optional;

public interface TrayMenuItemBuilder {

    MenuItem getMenuItem();

    String getLabel();

    ActionListener getActionListener();

    default MenuItem build() {
        return Optional.ofNullable(getMenuItem()).orElse(bulid(getLabel(), getActionListener()));
    }

    default MenuItem bulid(String label, ActionListener listener) {
        MenuItem item = new MenuItem();
        item.setLabel(label);
        item.addActionListener(listener);
        return item;
    }

}
