package com.urbanspork.client.mvc.component.tray.menu.item;

import java.awt.CheckboxMenuItem;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.TrayIcon.MessageType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.urbanspork.client.mvc.Components;
import com.urbanspork.client.mvc.Resource;
import com.urbanspork.client.mvc.component.Proxy;
import com.urbanspork.client.mvc.i18n.I18n;
import com.urbanspork.config.ClientConfig;
import com.urbanspork.config.ServerConfig;

public class ServersMenuItem implements TrayMenuItem {

    @Override
    public MenuItem getDirectly() {
        Menu menu = new Menu(I18n.TRAY_MENU_SERVERS);
        ClientConfig config = Resource.config();
        List<ServerConfig> servers = config.getServers();
        if (servers != null && !servers.isEmpty()) {
            final List<CheckboxMenuItem> items = new ArrayList<>();
            for (int j = 0; j < servers.size(); j++) {
                ServerConfig server = servers.get(j);
                CheckboxMenuItem item = new CheckboxMenuItem();
                item.setLabel(getLabel(server));
                if (config.getIndex() == j) {
                    item.setState(true);
                }
                item.addItemListener(l -> {
                    if (item.getState()) {
                        for (int k = 0; k < items.size(); k++) {
                            CheckboxMenuItem i = items.get(k);
                            if (i == item) {
                                config.setIndex(k);
                                Components.CONSOLE.serverConfigListViewSelect(k);
                            }
                            if (i != item && i.getState()) {
                                i.setState(false);
                            }
                        }
                        try {
                            config.save();
                        } catch (IOException e) {
                            Components.TRAY.displayMessage("Error", "Save file error, cause: " + e.getMessage(), MessageType.ERROR);
                            return;
                        }
                        Proxy.relaunch();
                        String message = config.getCurrent().toString();
                        Components.TRAY.displayMessage("Proxy is running", message, MessageType.INFO);
                        Components.TRAY.setToolTip(message);
                    } else {
                        item.setState(true);
                    }
                });
                items.add(item);
                menu.add(item);
            }
        }
        return menu;

    }

    @Override
    public String getLabel() {
        return I18n.TRAY_MENU_SERVERS;
    }

    @Override
    public void act() {

    }

    private String getLabel(ServerConfig config) {
        StringBuilder builder = new StringBuilder();
        builder.append(config.getHost()).append(':').append(config.getPort());
        if (config.getRemark() != null) {
            builder.insert(0, '(').insert(0, config.getRemark()).append(')');
        }
        return builder.toString();
    }

}