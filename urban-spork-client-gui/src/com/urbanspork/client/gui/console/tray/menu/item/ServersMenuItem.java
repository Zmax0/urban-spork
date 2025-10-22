package com.urbanspork.client.gui.console.tray.menu.item;

import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.Console;
import com.urbanspork.client.gui.i18n.I18N;
import com.urbanspork.client.gui.tray.Tray;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import javafx.application.Platform;

import javax.swing.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ItemListener;
import java.util.List;

public record ServersMenuItem(Console console, Tray tray) {

    public JMenuItem build() {
        JMenu menu = new JMenu(I18N.getString(I18N.TRAY_MENU_SERVERS));
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
        tray.changeSupport().addPropertyChangeListener(_ -> menu.setText(I18N.getString(I18N.TRAY_MENU_SERVERS)));
        return menu;
    }

    private ItemListener createItemListener(JRadioButtonMenuItem item, ClientConfig config, int index) {
        return _ -> {
            if (item.isSelected()) {
                config.setIndex(index);
                if (Platform.isFxApplicationThread()) {
                    console.getServerConfigJFXListView().getSelectionModel().select(index);
                } else {
                    Platform.runLater(() -> console.getServerConfigJFXListView().getSelectionModel().select(index));
                }
                try {
                    ConfigHandler.DEFAULT.save(config);
                } catch (Exception e) {
                    tray.displayMessage("Error", "Save file error, cause: " + e.getMessage(), MessageType.ERROR);
                    return;
                }
                console.launchProxy();
            }
        };
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