package com.urbanspork.client.gui.console.widget;

import com.urbanspork.client.ClientChannelTrafficHandler;
import com.urbanspork.client.gui.i18n.I18N;
import com.urbanspork.client.gui.util.HumanReadable;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Map;
import java.util.stream.Collectors;

public class ClientChannelTrafficTableView extends TableView<ClientChannelTrafficTableView.Row> {
    private final ObservableList<Row> list = FXCollections.observableArrayList();

    public ClientChannelTrafficTableView(ObjectProperty<Map<String, ClientChannelTrafficHandler>> channelTraffic) {
        channelTraffic.addListener((observable, oldValue, newValue) -> {
            this.list.clear();
            this.list.addAll(convert(channelTraffic).values());
        });
        initTableColumn();
        setItems(this.list);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        placeholderProperty().set(new StackPane());
        Timeline timeline = new Timeline(new KeyFrame(
            Duration.seconds(1), event -> {
            this.list.clear();
            this.list.addAll(convert(channelTraffic).values());
        }
        ));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void initTableColumn() {
        TableColumn<Row, String> hostCol = new TableColumn<>();
        TableColumn<Row, String> downloadedCol = new TableColumn<>();
        TableColumn<Row, String> uploadedCol = new TableColumn<>();
        TableColumn<Row, String> dlSpeedCol = new TableColumn<>();
        TableColumn<Row, String> ulSpeedCol = new TableColumn<>();
        hostCol.setPrefWidth(180);
        hostCol.textProperty().bind(I18N.binding(I18N.CHANNEL_TRAFFIC_TABLEVIEW_COLUMN_0_LABEL));
        downloadedCol.textProperty().bind(I18N.binding(I18N.CHANNEL_TRAFFIC_TABLEVIEW_COLUMN_1_LABEL));
        uploadedCol.textProperty().bind(I18N.binding(I18N.CHANNEL_TRAFFIC_TABLEVIEW_COLUMN_2_LABEL));
        dlSpeedCol.textProperty().bind(I18N.binding(I18N.CHANNEL_TRAFFIC_TABLEVIEW_COLUMN_3_LABEL));
        ulSpeedCol.textProperty().bind(I18N.binding(I18N.CHANNEL_TRAFFIC_TABLEVIEW_COLUMN_4_LABEL));
        hostCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().host));
        downloadedCol.setCellValueFactory(cellData -> new SimpleStringProperty(HumanReadable.byteCountSI(cellData.getValue().downloaded)));
        uploadedCol.setCellValueFactory(cellData -> new SimpleStringProperty(HumanReadable.byteCountSI(cellData.getValue().uploaded)));
        dlSpeedCol.setCellValueFactory(cellData -> new SimpleStringProperty(HumanReadable.byteCountSI(cellData.getValue().dlSpeed)));
        ulSpeedCol.setCellValueFactory(cellData -> new SimpleStringProperty(HumanReadable.byteCountSI(cellData.getValue().ulSpeed)));
        ObservableList<TableColumn<Row, ?>> columns = getColumns();
        columns.add(hostCol);
        columns.add(downloadedCol);
        columns.add(uploadedCol);
        columns.add(dlSpeedCol);
        columns.add(ulSpeedCol);
    }

    private static Map<String, Row> convert(ObjectProperty<Map<String, ClientChannelTrafficHandler>> channelTraffic) {
        return channelTraffic.getValue().values().stream().collect(Collectors.toMap(
            ClientChannelTrafficHandler::getHost, Row::new,
            (a, b) -> new Row(a.host, a.downloaded + b.downloaded, a.uploaded + b.downloaded, a.dlSpeed + b.dlSpeed, a.ulSpeed + b.ulSpeed)
        ));
    }

    record Row(String host, long downloaded, long uploaded, long dlSpeed, long ulSpeed) {
        Row(ClientChannelTrafficHandler handler) {
            this(handler.getHost(), handler.getDownloaded(), handler.getUploaded(), handler.getDlSpeed(), handler.getUlSpeed());
        }
    }
}
