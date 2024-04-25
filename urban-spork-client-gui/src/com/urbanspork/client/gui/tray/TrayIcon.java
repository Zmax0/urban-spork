package com.urbanspork.client.gui.tray;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class TrayIcon extends java.awt.TrayIcon {
    private JDialog dialog;

    public TrayIcon(Image image, String tooltip, Runnable onLeftClick, JPopupMenu popup) {
        super(image, tooltip);
        setImageAutoSize(true);
        // add mouse listener
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && onLeftClick != null) {
                    onLeftClick.run();
                }
                if (e.getButton() == MouseEvent.BUTTON3 && e.isPopupTrigger()) {
                    dialog.setLocation(adjustLocation(e.getPoint(), popup.getPreferredSize().getHeight()));
                    dialog.setVisible(true);
                    popup.show(dialog, 0, 0);
                }
            }
        });
        // add property change listener
        SystemTray.getSystemTray().addPropertyChangeListener("trayIcons", e -> {
            java.awt.TrayIcon[] oldArray = (java.awt.TrayIcon[]) e.getOldValue();
            java.awt.TrayIcon[] newArray = (java.awt.TrayIcon[]) e.getNewValue();
            if (contains(oldArray, this) && !contains(newArray, this)) {
                dialog.dispose();
            }
            if (!contains(oldArray, this) && contains(newArray, this)) {
                dialog = new JDialog();
                dialog.setUndecorated(true);
            }
        });
        // add popup menu listener
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // should do nothing
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                dialog.setVisible(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                dialog.setVisible(false);
            }
        });
    }

    private static Point adjustLocation(Point p, double menuHeight) {
        GraphicsDevice screenDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        Rectangle bounds = screenDevice.getDefaultConfiguration().getBounds();
        if (bounds.contains(p)) {
            return p;
        } else {
            double scale = screenDevice.getDisplayMode().getWidth() / bounds.getWidth();
            int x = (int) (p.getX() / scale);
            int y = (int) (p.getY() / scale - menuHeight);
            return new Point(x, y);
        }
    }

    private boolean contains(Object[] arr, Object obj) {
        if (arr == null || arr.length == 0) {
            return false;
        }
        return Arrays.asList(arr).contains(obj);
    }
}
