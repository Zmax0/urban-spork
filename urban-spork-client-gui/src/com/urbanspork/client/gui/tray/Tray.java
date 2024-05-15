package com.urbanspork.client.gui.tray;

public interface Tray {

    void displayMessage(String caption, String text, java.awt.TrayIcon.MessageType messageType);

    void setToolTip(String tooltip);

    void refresh();

    void exit();
}
