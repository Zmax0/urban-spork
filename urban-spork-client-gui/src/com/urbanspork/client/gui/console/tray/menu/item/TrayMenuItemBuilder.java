package com.urbanspork.client.gui.console.tray.menu.item;

import com.urbanspork.client.gui.i18n.I18N;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeSupport;

public interface TrayMenuItemBuilder {
    String getTextKey();

    ActionListener getActionListener();

    PropertyChangeSupport getPropertyChangeSupport();

    default String getText() {
        return I18N.getString(getTextKey());
    }

    default JMenuItem build() {
        JMenuItem item = new JMenuItem();
        item.setText(getText());
        item.addActionListener(getActionListener());
        getPropertyChangeSupport().addPropertyChangeListener(_ -> item.setText(getText()));
        return item;
    }
}
