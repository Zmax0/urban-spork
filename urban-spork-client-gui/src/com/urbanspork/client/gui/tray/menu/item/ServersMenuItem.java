package com.urbanspork.client.gui.tray.menu.item;

import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.component.Console;
import com.urbanspork.client.gui.console.component.Proxy;
import com.urbanspork.client.gui.console.component.Tray;
import com.urbanspork.client.gui.i18n.I18n;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ServersMenuItem implements TrayMenuItemBuilder {

    private final Console console;

    public ServersMenuItem(Console console) {
        this.console = console;
    }

    @Override
    public MenuItem getMenuItem() {
        Menu menu = new Menu(getLabel());
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
                item.addItemListener(listener -> itemStateChanged(config, items, item));
                items.add(item);
                menu.add(item);
            }
        }
        return menu;
    }

    private void itemStateChanged(ClientConfig config, List<CheckboxMenuItem> items, CheckboxMenuItem item) {
        if (item.getState()) {
            for (int k = 0; k < items.size(); k++) {
                CheckboxMenuItem i = items.get(k);
                if (i.equals(item)) {
                    config.setIndex(k);
                    console.getServerConfigJFXListView().getSelectionModel().select(k);
                }
                if (!i.equals(item) && i.getState()) {
                    i.setState(false);
                }
            }
            try {
                config.save();
            } catch (Exception e) {
                Tray.displayMessage("Error", "Save file error, cause: " + e.getMessage(), MessageType.ERROR);
                return;
            }
            Proxy.launch();
            String message = config.getCurrent().toString();
            Tray.displayMessage("Proxy is running", message, MessageType.INFO);
            Tray.setToolTip(message);
        } else {
            item.setState(true);
        }
    }

    @Override
    public ActionListener getActionListener() {
        return null;
    }

    @Override
    public String getLabel() {
        return I18n.TRAY_MENU_SERVERS;
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