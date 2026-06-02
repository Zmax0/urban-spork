package com.urbanspork.client.gui.console.widget;

import com.urbanspork.client.gui.i18n.I18N;
import com.urbanspork.common.config.ServerConfig;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

public class ServerConfigTableView extends TableView<ServerConfig> {
    public static final PseudoClass ACTIVE_SERVER = PseudoClass.getPseudoClass("active-server");

    public ServerConfigTableView() {
        initTableColumn();
        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        placeholderProperty().set(new StackPane());
    }

    private void initTableColumn() {
        TableColumn<ServerConfig, Number> indexCol = new TableColumn<>();
        TableColumn<ServerConfig, String> protocolCol = new TableColumn<>();
        TableColumn<ServerConfig, String> aliasCol = new TableColumn<>();
        TableColumn<ServerConfig, String> hostCol = new TableColumn<>();
        TableColumn<ServerConfig, String> portCol = new TableColumn<>();
        indexCol.setMaxWidth(30);
        protocolCol.setMinWidth(90);

        indexCol.textProperty().bind(I18N.binding(I18N.CONSOLE_TABLE_SERVER_COLUMN_INDEX));
        protocolCol.textProperty().bind(I18N.binding(I18N.CONSOLE_TABLE_SERVER_COLUMN_PROTOCOL));
        aliasCol.textProperty().bind(I18N.binding(I18N.CONSOLE_TABLE_SERVER_COLUMN_ALIAS));
        hostCol.textProperty().bind(I18N.binding(I18N.CONSOLE_TABLE_SERVER_COLUMN_HOST));
        portCol.textProperty().bind(I18N.binding(I18N.CONSOLE_TABLE_SERVER_COLUMN_PORT));

        indexCol.setCellValueFactory(_ -> new ReadOnlyObjectWrapper<>(0));
        indexCol.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : Integer.toString(getIndex() + 1));
            }
        });
        protocolCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProtocol() == null ? "" : cellData.getValue().getProtocol().name()));
        aliasCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().listItemText()));
        hostCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getHost()));
        portCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getPort())));

        configureColumn(indexCol, false);
        configureColumn(protocolCol, false);
        configureColumn(aliasCol, true);
        configureColumn(hostCol, true);
        configureColumn(portCol, false);

        ObservableList<TableColumn<ServerConfig, ?>> columns = getColumns();
        columns.add(indexCol);
        columns.add(protocolCol);
        columns.add(aliasCol);
        columns.add(hostCol);
        columns.add(portCol);
    }

    private void configureColumn(TableColumn<ServerConfig, ?> column, boolean resizable) {
        column.setReorderable(false);
        column.setSortable(false);
        column.setResizable(resizable);
    }
}
