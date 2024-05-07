package com.urbanspork.client.gui.tray;

import java.awt.TrayIcon;

public final class Unsupported implements Tray {
    @Override
    public void displayMessage(String caption, String text, TrayIcon.MessageType messageType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setToolTip(String tooltip) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exit() {
        throw new UnsupportedOperationException();
    }
}
