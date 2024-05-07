package com.urbanspork.client.gui.console.tray.menu.item;

import javax.swing.*;
import java.awt.event.ActionListener;

public interface TrayMenuItemBuilder {

    String getLabel();

    ActionListener getActionListener();

    default JMenuItem build() {
        JMenuItem item = new JMenuItem();
        item.setText(getLabel());
        item.addActionListener(getActionListener());
        return item;
    }
}
