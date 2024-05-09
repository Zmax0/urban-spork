package com.urbanspork.client.gui.tray;

import java.awt.TrayIcon;

public final class Unsupported implements Tray {
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
