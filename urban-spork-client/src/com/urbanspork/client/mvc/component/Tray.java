package com.urbanspork.client.mvc.component;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.ImageIcon;

import com.urbanspork.client.mvc.Component;
import com.urbanspork.client.mvc.Resource;
import com.urbanspork.client.mvc.i18n.I18n;
import com.urbanspork.config.ClientConfig;
import com.urbanspork.config.ConfigHandler;

import javafx.application.Platform;

public class Tray {

    private static final boolean isSupported = SystemTray.isSupported();

    private static TrayIcon trayIcon;

    public static void launch(String[] args) {
        if (isSupported) {
            // ==============================
            // tray menu
            // ==============================
            PopupMenu menu = new PopupMenu();
            MenuItem consoleItem = new MenuItem(I18n.TRAY_MENU_CONSOLE);
            MenuItem exitItem = new MenuItem(I18n.TRAY_EXIT);
            Menu languageMemu = new Menu(I18n.TRAY_MENU_LANGUAGE);
            buildLanguageMenu(languageMemu);
            menu.add(consoleItem);
            menu.addSeparator();
            menu.add(languageMemu);
            menu.addSeparator();
            menu.add(exitItem);
            // ==============================
            // tray icon
            // ==============================
            SystemTray tray = SystemTray.getSystemTray();
            ImageIcon icon = new ImageIcon(Resource.TRAY_ICON);
            trayIcon = new TrayIcon(icon.getImage(), I18n.PRAGRAM_TITLE, menu);
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                displayMessage("Error", e.getMessage(), MessageType.ERROR);
            }
            // ==============================
            // menu item listener
            // ==============================
            consoleItem.addActionListener(l -> {
                Platform.runLater(() -> {
                    Console console = Component.Console.get();
                    console.show();
                });
            });
            exitItem.addActionListener(l -> {
                Platform.exit();
                tray.remove(trayIcon);
                System.exit(0);
            });
        }
    }

    public static void displayMessage(String caption, String text, MessageType messageType) {
        if (isSupported && trayIcon != null) {
            trayIcon.displayMessage(caption, text, messageType);
        }
    }

    public static void setToolTip(String tooltip) {
        if (isSupported && trayIcon != null) {
            StringBuilder builder = new StringBuilder(I18n.TRAY_TOOLTIP);
            builder.append(System.lineSeparator()).append(tooltip);
            trayIcon.setToolTip(builder.toString());
        }
    }

    private static void buildLanguageMenu(Menu menu) {
        ClientConfig config = Resource.config;
        String language = config.getLanguage();
        final Locale configLanguage = new Locale(language);
        List<CheckboxMenuItem> items = new ArrayList<>(I18n.LANGUAGES.length);
        for (Locale locale : I18n.LANGUAGES) {
            CheckboxMenuItem item = new CheckboxMenuItem();
            item.setName(locale.getLanguage());
            item.setLabel(locale.getDisplayLanguage(configLanguage));
            if (locale.equals(configLanguage)) {
                item.setState(true);
            }
            item.addItemListener(l -> {
                if (item.getState()) {
                    config.setLanguage(item.getName());
                    try {
                        ConfigHandler.write(config);
                    } catch (IOException e) {
                        displayMessage("Error", "Save config error, cause: " + e.getMessage(), MessageType.ERROR);
                    }
                    displayMessage("Config is saved", "Take effect after restart", MessageType.INFO);
                    for (CheckboxMenuItem i : items) {
                        if (i != item && i.getState()) {
                            i.setState(false);
                        }
                    }
                } else {
                    item.setState(true);
                }
            });
            items.add(item);
            menu.add(item);
        }
    }

}
