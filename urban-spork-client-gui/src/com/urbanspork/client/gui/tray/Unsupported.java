package com.urbanspork.client.gui.tray;

import java.awt.TrayIcon;
import java.beans.PropertyChangeSupport;

public final class Unsupported implements Tray {
    @Override
    public PropertyChangeSupport changeSupport() {
        return null;
    }

    @Override
    public void displayMessage(String caption, String text, TrayIcon.MessageType messageType) {
        // do nothing
    }

    @Override
    public void setToolTip(String tooltip) {
        // do nothing
    }

    @Override
    public void refresh() {
        // do nothing
    }

    @Override
    public void exit() {
        // do nothing
    }
}
