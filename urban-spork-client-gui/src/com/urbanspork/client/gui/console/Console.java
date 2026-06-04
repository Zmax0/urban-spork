package com.urbanspork.client.gui.console;

import com.urbanspork.client.ClientChannelTrafficHandler;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.widget.ClientChannelTrafficTableView;
import com.urbanspork.client.gui.console.widget.ConsoleLabel;
import com.urbanspork.client.gui.console.widget.ConsoleRowConstraints;
import com.urbanspork.client.gui.console.widget.NumericTextField;
import com.urbanspork.client.gui.console.widget.ServerConfigTableView;
import com.urbanspork.client.gui.i18n.I18N;
import com.urbanspork.client.gui.traffic.TrafficCounterLineChartBackstage;
import com.urbanspork.client.gui.tray.Tray;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.shadowsocks.ShareableServerConfig;
import io.netty.handler.traffic.TrafficCounter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Console extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Console.class);
    private static final ClientConfig CLIENT_CONFIG = Resource.config();

    Tray tray;
    Proxy proxy;
    final ObjectProperty<TrafficCounter> trafficCounter = new SimpleObjectProperty<>();
    final ObjectProperty<Map<String, ClientChannelTrafficHandler>> channelTraffic = new SimpleObjectProperty<>();
    final TrafficCounterLineChartBackstage trafficCounterLineChartBackstage = new TrafficCounterLineChartBackstage();

    private Stage primaryStage;
    private TabPane root;
    private Tab trafficTab;
    private TextArea logTextArea;
    private ServerConfigTableView serverConfigTableView;
    private ObservableList<ServerConfig> serverConfigObservableList;
    private NumericTextField clientConfigPortTextField;
    private ServerConfigDialog serverConfigDialog;

    @Override
    public void init() {
        initModule();
        initController();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        Platform.setImplicitExit(false);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(515);
        primaryStage.setMinHeight(555);
        primaryStage.getIcons().add(new Image(Resource.PROGRAM_ICON.toString()));
        primaryStage.titleProperty().bind(I18N.binding(I18N.PROGRAM_TITLE));
        primaryStage.setOnCloseRequest(_ -> primaryStage.hide());
        primaryStage.hide();
        initOnJavaFxApplicationThread();
        launchProxy();
    }

    private void initOnJavaFxApplicationThread() {
        initTrafficComponents();
        serverConfigDialog = new ServerConfigDialog(primaryStage);
    }

    @Override
    public void stop() {
        primaryStage.hide();
        proxy.exit();
        tray.exit();
    }

    public void show() {
        if (primaryStage.isIconified()) {
            primaryStage.setIconified(false);
        }
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        } else {
            primaryStage.toFront();
        }
    }

    public void selectServerConfig(int index) {
        if (index >= 0 && index < serverConfigObservableList.size()) {
            serverConfigTableView.getSelectionModel().select(index);
            serverConfigTableView.scrollTo(index);
        }
    }

    public void newServerConfig() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setProtocol(com.urbanspork.common.protocol.Protocol.shadowsocks);
        serverConfig.setCipher(com.urbanspork.common.codec.CipherKind.aes_256_gcm);
        serverConfigDialog.show(serverConfig).ifPresent(result -> {
            serverConfigObservableList.add(result);
            saveConfig();
            selectServerConfig(serverConfigObservableList.size() - 1);
        });
    }

    public void editSelectedServerConfig() {
        int index = getSelectedServerIndex();
        if (index < 0) {
            return;
        }
        ServerConfig original = serverConfigObservableList.get(index);
        cloneConfig(original).flatMap(serverConfigDialog::show).ifPresent(result -> {
            serverConfigObservableList.set(index, result);
            if (CLIENT_CONFIG.getIndex() == index) {
                launchProxy();
            }
            saveConfig();
            serverConfigTableView.refresh();
            selectServerConfig(index);
        });
    }

    public void deleteServerConfig() {
        int index = getSelectedServerIndex();
        if (index <= 0) {
            return;
        }
        boolean restart = CLIENT_CONFIG.getIndex() >= index;
        serverConfigObservableList.remove(index);
        adjustActiveIndexAfterDelete(index);
        saveConfig();
        serverConfigTableView.refresh();
        selectServerConfig(Math.min(index, serverConfigObservableList.size() - 1));
        if (restart) {
            launchProxy();
        }
    }

    public void copyServerConfig() {
        int index = getSelectedServerIndex();
        if (index < 0) {
            return;
        }
        cloneConfig(serverConfigObservableList.get(index)).ifPresent(copied -> {
            serverConfigObservableList.add(index + 1, copied);
            adjustActiveIndexForInsert(index + 1);
            saveConfig();
            selectServerConfig(index + 1);
        });
    }

    public void moveUpServerConfig() {
        moveServerConfig(-1);
    }

    public void moveDownServerConfig() {
        moveServerConfig(1);
    }

    public void setActiveServerConfig() {
        int index = getSelectedServerIndex();
        if (index < 0 || CLIENT_CONFIG.getIndex() == index) {
            return;
        }
        CLIENT_CONFIG.setIndex(index);
        saveConfig();
        serverConfigTableView.refresh();
        launchProxy();
    }

    private void initWidget() {
        serverConfigTableView = new ServerConfigTableView();
        serverConfigTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        logTextArea = initLogTextArea();
        clientConfigPortTextField = new NumericTextField();
        clientConfigPortTextField.bindRequiredMessage(I18N.binding(I18N.CONSOLE_VALIDATOR_REQUIRED_FIELD_MESSAGE));
    }

    private TextArea initLogTextArea() {
        if (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) instanceof ch.qos.logback.classic.Logger log
            && log.getAppender(Resource.application().getString("console.log.appender.name")) instanceof Appender appender) {
            return appender.getTextArea();
        }
        logger.warn("Log text area is missing");
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        return textArea;
    }

    private TabPane initTabPane() {
        Tab serverTab = initServerTab();
        Tab logTab = newSingleNodeTab(logTextArea, I18N.binding(I18N.CONSOLE_TAB1_TEXT));
        trafficTab = initTrafficTab();
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(serverTab, logTab, trafficTab);
        tabPane.getStylesheets().add(Resource.CONSOLE_CSS.toExternalForm());
        return tabPane;
    }

    private Tab initServerTab() {
        GridPane gridPane = new GridPane();
        ColumnConstraints cGap = new ColumnConstraints(10);
        RowConstraints rGap = new RowConstraints(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(95);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        RowConstraints tableRow = new RowConstraints();
        tableRow.setVgrow(Priority.ALWAYS);
        ConsoleRowConstraints proxyPortRow = new ConsoleRowConstraints(32);
        proxyPortRow.setValignment(VPos.CENTER);
        gridPane.getColumnConstraints().addAll(cGap, labelCol, cGap, fieldCol, cGap);
        gridPane.getRowConstraints().addAll(rGap, tableRow, rGap, proxyPortRow, rGap);

        gridPane.add(serverConfigTableView, 1, 1, 3, 1);
        gridPane.add(new ConsoleLabel(I18N.binding(I18N.CONSOLE_LABEL_PROXY_PORT)), 1, 3);
        gridPane.add(clientConfigPortTextField, 3, 3);
        GridPane.setValignment(clientConfigPortTextField, VPos.CENTER);
        GridPane.setFillHeight(clientConfigPortTextField, false);

        Tab tab = new Tab();
        tab.textProperty().bind(I18N.binding(I18N.CONSOLE_TAB0_TEXT));
        tab.setContent(gridPane);
        tab.setClosable(false);
        return tab;
    }

    private Tab newSingleNodeTab(Node node, StringBinding tabTitle) {
        GridPane gridPane = new GridPane();
        ColumnConstraints cGap = new ColumnConstraints(10);
        RowConstraints rGap = new RowConstraints(10);
        ColumnConstraints cAlways = new ColumnConstraints();
        cAlways.setHgrow(Priority.ALWAYS);
        RowConstraints rAlways = new RowConstraints();
        rAlways.setVgrow(Priority.ALWAYS);
        ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
        columnConstraints.add(cGap);
        columnConstraints.add(cAlways);
        columnConstraints.add(cGap);
        ObservableList<RowConstraints> rowConstraints = gridPane.getRowConstraints();
        rowConstraints.add(rGap);
        rowConstraints.add(rAlways);
        rowConstraints.add(rGap);
        gridPane.add(node, 1, 1);
        Tab tab = new Tab();
        tab.textProperty().bind(tabTitle);
        tab.setContent(gridPane);
        tab.setClosable(false);
        return tab;
    }

    private Tab initTrafficTab() {
        VBox vBox = new VBox();
        Tab tab = new Tab();
        tab.textProperty().bind(I18N.binding(I18N.CONSOLE_TAB2_TEXT));
        tab.setContent(vBox);
        tab.setClosable(false);
        return tab;
    }

    private void initModule() {
        initWidget();
        root = initTabPane();
    }

    private void initController() {
        initServerConfigTableView();
        initClientConfigPortTextField();
        display();
    }

    private void initClientConfigPortTextField() {
        clientConfigPortTextField.textProperty().addListener((_, _, _) -> clientConfigPortTextField.validate());
        clientConfigPortTextField.inputFocusedProperty().addListener((_, _, newValue) -> {
            if (!newValue) {
                clientConfigPortTextField.getIntValue().ifPresent(port -> {
                    if (CLIENT_CONFIG.getPort() != port) {
                        CLIENT_CONFIG.setPort(port);
                        launchProxy();
                        saveConfig();
                    }
                });
            }
        });
    }

    private void initServerConfigTableView() {
        List<ServerConfig> servers = CLIENT_CONFIG.getServers();
        serverConfigObservableList = FXCollections.observableArrayList(servers == null ? List.of() : servers);
        CLIENT_CONFIG.setServers(serverConfigObservableList);
        serverConfigTableView.setItems(serverConfigObservableList);
        serverConfigTableView.getSelectionModel().clearSelection();
        serverConfigTableView.setContextMenu(createTableContextMenu());
        serverConfigTableView.setRowFactory(_ -> {
            TableRow<ServerConfig> row = new TableRow<>();
            row.itemProperty().addListener((_, _, item) -> {
                boolean active = item != null && row.getIndex() == CLIENT_CONFIG.getIndex();
                row.pseudoClassStateChanged(ServerConfigTableView.ACTIVE_SERVER, active);
                row.setContextMenu(item == null ? null : createRowContextMenu());
            });
            row.indexProperty().addListener((_, _, _) -> {
                boolean active = !row.isEmpty() && row.getItem() != null && row.getIndex() == CLIENT_CONFIG.getIndex();
                row.pseudoClassStateChanged(ServerConfigTableView.ACTIVE_SERVER, active);
            });
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    serverConfigTableView.getSelectionModel().select(row.getIndex());
                    editSelectedServerConfig();
                }
            });
            row.setOnContextMenuRequested(_ -> {
                if (!row.isEmpty()) {
                    serverConfigTableView.getSelectionModel().select(row.getIndex());
                }
            });
            return row;
        });
    }

    private ContextMenu createTableContextMenu() {
        MenuItem newServerConfigMenuItem = new MenuItem();
        newServerConfigMenuItem.textProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_NEW));
        newServerConfigMenuItem.setOnAction(_ -> newServerConfig());
        MenuItem importServerConfigMenuItem = new MenuItem();
        importServerConfigMenuItem.textProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_IMPORT));
        importServerConfigMenuItem.setOnAction(_ -> importServerConfig());
        ContextMenu contextMenu = new ContextMenu(newServerConfigMenuItem, importServerConfigMenuItem);
        contextMenu.setOnShowing(_ -> serverConfigTableView.getSelectionModel().clearSelection());
        return contextMenu;
    }

    private ContextMenu createRowContextMenu() {
        MenuItem newServerConfigMenuItem = new MenuItem();
        MenuItem editServerConfigMenuItem = new MenuItem();
        MenuItem copyServerConfigMenuItem = new MenuItem();
        MenuItem deleteServerConfigMenuItem = new MenuItem();
        MenuItem moveUpServerConfigMenuItem = new MenuItem();
        MenuItem moveDownServerConfigMenuItem = new MenuItem();
        MenuItem importServerConfigMenuItem = new MenuItem();
        MenuItem shareServerConfigMenuItem = new MenuItem();
        MenuItem setActiveServerConfigMenuItem = new MenuItem();

        newServerConfigMenuItem.textProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_NEW));
        editServerConfigMenuItem.textProperty().bind(I18N.binding(I18N.CONSOLE_CONTEXT_MENU_EDIT));
        copyServerConfigMenuItem.textProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_COPY));
        deleteServerConfigMenuItem.textProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_DEL));
        moveUpServerConfigMenuItem.textProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_UP));
        moveDownServerConfigMenuItem.textProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_DOWN));
        importServerConfigMenuItem.textProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_IMPORT));
        shareServerConfigMenuItem.textProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_SHARE));
        setActiveServerConfigMenuItem.textProperty().bind(I18N.binding(I18N.CONSOLE_CONTEXT_MENU_SET_ACTIVE));

        newServerConfigMenuItem.setOnAction(_ -> newServerConfig());
        editServerConfigMenuItem.setOnAction(_ -> editSelectedServerConfig());
        copyServerConfigMenuItem.setOnAction(_ -> copyServerConfig());
        deleteServerConfigMenuItem.setOnAction(_ -> deleteServerConfig());
        moveUpServerConfigMenuItem.setOnAction(_ -> moveUpServerConfig());
        moveDownServerConfigMenuItem.setOnAction(_ -> moveDownServerConfig());
        importServerConfigMenuItem.setOnAction(_ -> importServerConfig());
        shareServerConfigMenuItem.setOnAction(_ -> shareServerConfig());
        setActiveServerConfigMenuItem.setOnAction(_ -> setActiveServerConfig());

        ContextMenu contextMenu = new ContextMenu(
            newServerConfigMenuItem,
            editServerConfigMenuItem,
            copyServerConfigMenuItem,
            deleteServerConfigMenuItem,
            moveUpServerConfigMenuItem,
            moveDownServerConfigMenuItem,
            importServerConfigMenuItem,
            shareServerConfigMenuItem,
            setActiveServerConfigMenuItem
        );
        contextMenu.setOnShowing(_ -> {
            int index = getSelectedServerIndex();
            boolean hasSelection = index >= 0;
            boolean isShadowsocks = hasSelection && serverConfigObservableList.get(index).getProtocol() == com.urbanspork.common.protocol.Protocol.shadowsocks;
            editServerConfigMenuItem.setDisable(!hasSelection);
            copyServerConfigMenuItem.setDisable(!hasSelection);
            deleteServerConfigMenuItem.setDisable(!hasSelection || index == 0);
            moveUpServerConfigMenuItem.setDisable(!hasSelection || index == 0);
            moveDownServerConfigMenuItem.setDisable(!hasSelection || index >= serverConfigObservableList.size() - 1);
            shareServerConfigMenuItem.setDisable(!isShadowsocks);
            setActiveServerConfigMenuItem.setDisable(!hasSelection || CLIENT_CONFIG.getIndex() == index);
        });
        return contextMenu;
    }

    private void importServerConfig() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setGraphic(null);
        dialog.titleProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_IMPORT));
        dialog.setHeaderText(null);
        dialog.showAndWait().map(URI::create).flatMap(ShareableServerConfig::fromUri).ifPresent(config -> {
            serverConfigObservableList.add(config);
            saveConfig();
            selectServerConfig(serverConfigObservableList.size() - 1);
        });
    }

    private void shareServerConfig() {
        int index = getSelectedServerIndex();
        if (index < 0) {
            return;
        }
        ShareableServerConfig.produceUri(serverConfigObservableList.get(index)).ifPresent(uri -> {
            String string = uri.toString();
            TextInputDialog dialog = new TextInputDialog();
            dialog.setGraphic(null);
            dialog.titleProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_SHARE));
            dialog.setHeaderText(null);
            dialog.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
            dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);
            dialog.getDialogPane().setPrefWidth(string.length() * 8);
            TextField editor = dialog.getEditor();
            editor.setText(string);
            editor.setEditable(false);
            dialog.show();
        });
    }

    private void initTrafficComponents() {
        ObservableList<Node> children = ((VBox) trafficTab.getContent()).getChildren();
        ClientChannelTrafficTableView tableView = new ClientChannelTrafficTableView(channelTraffic);
        primaryStage.setOnHidden(_ -> {
            children.clear();
            trafficCounterLineChartBackstage.stop();
            tableView.stop();
        });
        primaryStage.setOnShown(_ -> {
            if (children.isEmpty()) {
                children.add(trafficCounterLineChartBackstage.newLineChart());
                children.add(tableView);
                VBox.setVgrow(tableView, Priority.SOMETIMES);
                tableView.play();
            }
            Optional.of(trafficCounter).map(ObservableObjectValue::get).ifPresent(trafficCounterLineChartBackstage::refresh);
        });
    }

    private void display() {
        clientConfigPortTextField.setText(CLIENT_CONFIG.getPort());
        serverConfigTableView.refresh();
    }

    private void saveConfig() {
        ConfigHandler.DEFAULT.save(CLIENT_CONFIG);
        tray.refresh();
    }

    public void launchProxy() {
        proxy.launch().ifPresent(instance -> {
            trafficCounter.set(instance.traffic());
            channelTraffic.set(instance.channelTraffic());
            serverConfigTableView.refresh();
        });
    }

    private void moveServerConfig(int offset) {
        int index = getSelectedServerIndex();
        int target = index + offset;
        if (index < 0 || target < 0 || target >= serverConfigObservableList.size()) {
            return;
        }
        ServerConfig config = serverConfigObservableList.remove(index);
        serverConfigObservableList.add(target, config);
        adjustActiveIndexAfterMove(index, target);
        saveConfig();
        serverConfigTableView.refresh();
        selectServerConfig(target);
    }

    private int getSelectedServerIndex() {
        return serverConfigTableView.getSelectionModel().getSelectedIndex();
    }

    private void adjustActiveIndexAfterDelete(int removedIndex) {
        int activeIndex = CLIENT_CONFIG.getIndex();
        if (activeIndex > removedIndex) {
            CLIENT_CONFIG.setIndex(activeIndex - 1);
        } else if (activeIndex >= serverConfigObservableList.size()) {
            CLIENT_CONFIG.setIndex(Math.max(serverConfigObservableList.size() - 1, 0));
        }
    }

    private void adjustActiveIndexForInsert(int insertedIndex) {
        if (CLIENT_CONFIG.getIndex() >= insertedIndex) {
            CLIENT_CONFIG.setIndex(CLIENT_CONFIG.getIndex() + 1);
        }
    }

    private void adjustActiveIndexAfterMove(int fromIndex, int toIndex) {
        int activeIndex = CLIENT_CONFIG.getIndex();
        if (activeIndex == fromIndex) {
            CLIENT_CONFIG.setIndex(toIndex);
        } else if (activeIndex > fromIndex && activeIndex <= toIndex) {
            CLIENT_CONFIG.setIndex(activeIndex - 1);
        } else if (activeIndex >= toIndex && activeIndex < fromIndex) {
            CLIENT_CONFIG.setIndex(activeIndex + 1);
        }
    }

    private Optional<ServerConfig> cloneConfig(ServerConfig config) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return Optional.of(mapper.readValue(mapper.writeValueAsBytes(config), ServerConfig.class));
        } catch (RuntimeException e) {
            logger.error("Clone server config failed", e);
            return Optional.empty();
        }
    }
}
