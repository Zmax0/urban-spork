package com.urbanspork.client.mvc.tray.menu.item;

import java.awt.CheckboxMenuItem;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.TrayIcon.MessageType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.urbanspork.client.mvc.Components;
import com.urbanspork.client.mvc.Resource;
import com.urbanspork.client.mvc.i18n.I18n;
import com.urbanspork.config.ClientConfig;
import com.urbanspork.config.ConfigHandler;

public class LanguageMenuItem implements TrayMenuItem {

    @Override
    public MenuItem getDirectly() {
        Menu menu = new Menu(I18n.TRAY_MENU_LANGUAGE);
        ClientConfig config = Resource.config();
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
                        Components.TRAY.displayMessage("Error", "Save file error, cause: " + e.getMessage(), MessageType.ERROR);
                        return;
                    }
                    Components.TRAY.displayMessage("Config is saved", "Take effect after restart", MessageType.INFO);
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
        return menu;
    }

    @Override
    public String getLabel() {
        return I18n.TRAY_MENU_LANGUAGE;
    }

    @Override
    public void act() {

    }

}