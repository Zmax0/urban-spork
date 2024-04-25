package com.urbanspork.client.gui.tray.menu.item;

import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.component.Tray;
import com.urbanspork.client.gui.i18n.I18N;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;

import javax.swing.*;
import java.awt.TrayIcon.MessageType;
import java.util.Locale;

public class LanguageMenuItem {
    public JMenuItem build() {
        JMenu menu = new JMenu(getLabel());
        ClientConfig config = Resource.config();
        String language = config.getLanguage();
        final Locale configLanguage = Locale.of(language);
        Locale[] languages = I18N.languages();
        ButtonGroup group = new ButtonGroup();
        for (Locale locale : languages) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem();
            item.setName(locale.getLanguage());
            item.setText(locale.getDisplayLanguage(configLanguage));
            if (locale.equals(configLanguage)) {
                item.setSelected(true);
            }
            item.addActionListener(evt -> {
                if (item.isSelected()) {
                    config.setLanguage(item.getName());
                    try {
                        ConfigHandler.DEFAULT.save(config);
                    } catch (Exception e) {
                        Tray.displayMessage("Error", "Save file error, cause: " + e.getMessage(), MessageType.ERROR);
                        return;
                    }
                    Tray.displayMessage("Config is saved", "Take effect after restart", MessageType.INFO);
                }
            });
            group.add(item);
            menu.add(item);
        }
        return menu;
    }

    private String getLabel() {
        return I18N.getString(I18N.TRAY_MENU_LANGUAGE);
    }
}