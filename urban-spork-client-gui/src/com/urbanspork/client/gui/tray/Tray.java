package com.urbanspork.client.gui.tray;

import java.beans.PropertyChangeSupport;

public interface Tray {
    PropertyChangeSupport changeSupport();

    void displayMessage(String caption, String text, java.awt.TrayIcon.MessageType messageType);

    void setToolTip(String tooltip);

    void refresh();

    void exit();
}
