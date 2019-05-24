package com.urbanspork.client.mvc.component.tray.menu.item;

import java.awt.MenuItem;

public interface TrayMenuItem {

    MenuItem getDirectly();

    String getLabel();

    void act();

    default MenuItem get() {
        MenuItem item = getDirectly();
        if (item == null) {
            item = new MenuItem();
            item.setLabel(getLabel());
            item.addActionListener(l -> {
                act();
            });
        }
        return item;
    }

}
