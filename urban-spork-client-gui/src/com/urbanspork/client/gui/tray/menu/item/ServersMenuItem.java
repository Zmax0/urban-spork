package com.urbanspork.client.gui.tray.menu.item;

import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.component.Console;
import com.urbanspork.client.gui.console.component.Proxy;
import com.urbanspork.client.gui.console.component.Tray;
import com.urbanspork.client.gui.i18n.I18N;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;

import javax.swing.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ItemListener;
import java.util.List;

public class ServersMenuItem {

    private final Console console;

    public ServersMenuItem(Console console) {
        this.console = console;
    }

    public JMenuItem build() {
        JMenu menu = new JMenu(getLabel());
        ClientConfig config = Resource.config();
        List<ServerConfig> servers = config.getServers();
        ButtonGroup group = new ButtonGroup();
        if (servers != null && !servers.isEmpty()) {
            for (int i = 0; i < servers.size(); i++) {
                ServerConfig server = servers.get(i);
                JRadioButtonMenuItem item = new JRadioButtonMenuItem();
                item.setText(getLabel(server));
                if (config.getIndex() == i) {
                    item.setSelected(true);
                }
                item.addItemListener(createItemListener(item, config, i));
                group.add(item);
                menu.add(item);
            }
        }
        return menu;
    }

    private ItemListener createItemListener(JRadioButtonMenuItem item, ClientConfig config, int index) {
        return event -> {
            if (item.isSelected()) {
                config.setIndex(index);
                console.getServerConfigJFXListView().getSelectionModel().select(index);
                try {
                    ConfigHandler.DEFAULT.save(config);
                } catch (Exception e) {
                    Tray.displayMessage("Error", "Save file error, cause: " + e.getMessage(), MessageType.ERROR);
                    return;
                }
                Proxy.launch();
            }
        };
    }

    private String getLabel() {
        return I18N.getString(I18N.TRAY_MENU_SERVERS);
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