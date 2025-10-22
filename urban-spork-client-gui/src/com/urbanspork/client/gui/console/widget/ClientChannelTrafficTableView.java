package com.urbanspork.client.gui.console.widget;

import com.urbanspork.client.ClientChannelTrafficHandler;
import com.urbanspork.client.gui.i18n.I18N;
import com.urbanspork.client.gui.util.HumanReadable;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import javafx.animation.Animation;
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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClientChannelTrafficTableView extends TableView<ClientChannelTrafficTableView.Row> {
    private final ObservableList<Row> list = FXCollections.observableArrayList();
    private final Timeline timeline = new Timeline();

    public ClientChannelTrafficTableView(ObjectProperty<Map<String, ClientChannelTrafficHandler>> channelTraffic) {
        timeline.setCycleCount(Animation.INDEFINITE);
        initTableColumn();
        setItems(this.list);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        placeholderProperty().set(new StackPane());
        channelTraffic.addListener((_, _, _) -> {
            ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            keyFrames.clear();
            keyFrames.add(new KeyFrame(
                channelTraffic.get().values().stream().findFirst().map(AbstractTrafficShapingHandler::trafficCounter).map(TrafficCounter::checkInterval).map(Duration::millis)
                    .orElse(Duration.seconds(1)),
                _ -> this.list.setAll(convert(channelTraffic).values())
            ));
            this.list.setAll(convert(channelTraffic).values());
        });
    }

    public void play() {
        timeline.playFromStart();
    }

    public void stop() {
        timeline.stop();
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
        return Optional.ofNullable(channelTraffic.getValue()).map(m -> m.values().stream().collect(Collectors.toMap(
            ClientChannelTrafficHandler::getHost, Row::new,
            (a, b) -> new Row(a.host, a.downloaded + b.downloaded, a.uploaded + b.uploaded, a.dlSpeed + b.dlSpeed, a.ulSpeed + b.ulSpeed)
        ))).orElse(Collections.emptyMap());
    }

    record Row(String host, long downloaded, long uploaded, long dlSpeed, long ulSpeed) {
        Row(ClientChannelTrafficHandler handler) {
            this(handler.getHost(), handler.getDownloaded(), handler.getUploaded(), handler.getDlSpeed(), handler.getUlSpeed());
        }
    }
}
