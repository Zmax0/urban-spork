package com.urbanspork.client.gui.console.widget;

import com.jfoenix.controls.JFXListCell;
import com.jfoenix.controls.JFXListView;
import com.urbanspork.common.config.ServerConfig;

public class ServerConfigListView extends JFXListView<ServerConfig> {

    public ServerConfigListView() {
        this.setCellFactory(param -> new JFXListCell<>() {
            @Override
            protected void updateItem(ServerConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    setText(item.listItemText());
                }
            }
        });
    }
}
