package com.urbanspork.client.gui.tray.menu.item;

import java.awt.CheckboxMenuItem;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.component.Tray;
import com.urbanspork.client.gui.i18n.I18n;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;

public class LanguageMenuItem implements TrayMenuItemBuilder {

    @Override
    public MenuItem getMenuItem() {
        Menu menu = new Menu(getLabel());
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
                        Tray.displayMessage("Error", "Save file error, cause: " + e.getMessage(), MessageType.ERROR);
                        return;
                    }
                    Tray.displayMessage("Config is saved", "Take effect after restart", MessageType.INFO);
                    for (CheckboxMenuItem i : items) {
                        if (!i.equals(item) && i.getState()) {
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
    public ActionListener getActionListener() {
        return null;
    }

}