package com.urbanspork.client.gui.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.RequiredFieldValidator;
import com.urbanspork.client.ClientChannelTrafficHandler;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.widget.*;
import com.urbanspork.client.gui.i18n.I18N;
import com.urbanspork.client.gui.traffic.TrafficCounterLineChartBackstage;
import com.urbanspork.client.gui.tray.Tray;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.shadowsocks.ShareableServerConfig;
import com.urbanspork.common.protocol.Protocol;
import io.netty.handler.traffic.TrafficCounter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
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
    final TrafficCounterLineChartBackstage trafficCounterLineChartBackstage = new TrafficCounterLineChartBackstage(trafficCounter);
    private final RequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator();

    private Stage primaryStage;
    private JFXTabPane root;
    private Tab tab2;
    private TextArea logTextArea;
    private JFXListView<ServerConfig> serverConfigJFXListView;
    private Button newServerConfigButton;
    private Button delServerConfigButton;
    private Button copyServerConfigButton;
    private Button importServerConfigButton;
    private Button shareServerConfigButton;
    private Button moveUpServerConfigButton;
    private Button moveDownServerConfigButton;
    private Button confirmServerConfigButton;
    private Button cancelServerConfigButton;
    private JFXTextField currentConfigHostTextField;
    private NumericTextField currentConfigPortTextField;
    private JFXPasswordField currentConfigPasswordPasswordField;
    private JFXTextField currentConfigPasswordTextField;
    private JFXTextField currentConfigRemarkTextField;
    private ToggleButton currentConfigPasswordToggleButton;
    private ChoiceBox<CipherKind> currentConfigCipherChoiceBox;
    private ChoiceBox<Protocol> currentConfigProtocolChoiceBox;
    private NumericTextField clientConfigPortTextField;
    private ObservableList<ServerConfig> serverConfigObservableList;

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
        primaryStage.setOnCloseRequest(event -> primaryStage.hide());
        primaryStage.hide();
        initTrafficCounterLineChart();
        launchProxy();
    }

    @Override
    public void stop() {
        tray.exit();
        proxy.exit();
    }

    public void hide() {
        primaryStage.hide();
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

    public JFXListView<ServerConfig> getServerConfigJFXListView() {
        return serverConfigJFXListView;
    }

    public void newServerConfig() {
        if (validate()) {
            ServerConfig newValue = new ServerConfig();
            newValue.setCipher(CipherKind.aes_256_gcm);
            serverConfigObservableList.add(newValue);
            serverConfigJFXListView.getSelectionModel().select(newValue);
            display(newValue);
        }
    }

    public void deleteServerConfig() {
        int index = serverConfigJFXListView.getSelectionModel().getSelectedIndex();
        if (index > 0) {
            serverConfigObservableList.remove(index);
            serverConfigJFXListView.getSelectionModel().select(index);
        }
    }

    public void copyServerConfig() {
        ServerConfig config = serverConfigJFXListView.getSelectionModel().getSelectedItem();
        if (config != null) {
            ObjectMapper mapper = new ObjectMapper();
            ServerConfig copied;
            try {
                copied = mapper.readValue(mapper.writeValueAsBytes(config), ServerConfig.class);
                serverConfigObservableList.add(copied);
            } catch (IOException e) {
                logger.error("Copy server config error", e);
            }
        }
    }

    public void moveUpServerConfig() {
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigJFXListView.getSelectionModel();
        int index = selectionModel.getSelectedIndex();
        if (index > 0) {
            ServerConfig config = serverConfigObservableList.get(index);
            serverConfigObservableList.remove(index);
            serverConfigObservableList.add(index - 1, config);
            selectionModel.select(index - 1);
        }
    }

    public void moveDownServerConfig() {
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigJFXListView.getSelectionModel();
        int index = selectionModel.getSelectedIndex();
        if (index < serverConfigObservableList.size() - 1) {
            ServerConfig config = serverConfigObservableList.get(index);
            serverConfigObservableList.remove(index);
            serverConfigObservableList.add(index + 1, config);
            selectionModel.select(index + 1);
        }
    }

    public void showCurrentConfigPassword() {
        if (currentConfigPasswordPasswordField.isVisible()) {
            currentConfigPasswordTextField.setText(currentConfigPasswordPasswordField.getText());
            currentConfigPasswordTextField.validate();
        } else {
            currentConfigPasswordPasswordField.setText(currentConfigPasswordTextField.getText());
            currentConfigPasswordPasswordField.validate();
        }
        currentConfigPasswordPasswordField.visibleProperty().set(!currentConfigPasswordToggleButton.isSelected());
        currentConfigPasswordTextField.visibleProperty().set(currentConfigPasswordToggleButton.isSelected());
    }

    public void confirmServerConfig() {
        if (validate()) {
            MultipleSelectionModel<ServerConfig> selectionModel = serverConfigJFXListView.getSelectionModel();
            ServerConfig config = selectionModel.getSelectedItem();
            boolean isNew = config == null;
            if (config == null) {
                config = new ServerConfig();
                config.setCipher(CipherKind.aes_128_gcm);
            }
            pack(config);
            if (isNew) {
                serverConfigObservableList.add(config);
                serverConfigJFXListView.getSelectionModel().select(config);
            } else {
                serverConfigJFXListView.refresh();
            }
            CLIENT_CONFIG.setPort(clientConfigPortTextField.getIntValue());
            CLIENT_CONFIG.setIndex(selectionModel.getSelectedIndex());
            saveConfig();
            launchProxy();
        }
    }

    private void initWidget() {
        serverConfigJFXListView = new ServerConfigListView();
        logTextArea = initLogTextArea();
        newServerConfigButton = new ConsoleButton(I18N.binding(I18N.CONSOLE_BUTTON_NEW), event -> newServerConfig());
        delServerConfigButton = new ConsoleButton(I18N.binding(I18N.CONSOLE_BUTTON_DEL), event -> deleteServerConfig());
        copyServerConfigButton = new ConsoleButton(I18N.binding(I18N.CONSOLE_BUTTON_COPY), event -> copyServerConfig());
        importServerConfigButton = new ConsoleButton(I18N.binding(I18N.CONSOLE_BUTTON_IMPORT), event -> importServerConfig());
        shareServerConfigButton = new ConsoleButton(I18N.binding(I18N.CONSOLE_BUTTON_SHARE), event -> shareServerConfig());
        moveUpServerConfigButton = new ConsoleLiteButton(I18N.binding(I18N.CONSOLE_BUTTON_UP), event -> moveUpServerConfig());
        moveDownServerConfigButton = new ConsoleLiteButton(I18N.binding(I18N.CONSOLE_BUTTON_DOWN), event -> moveDownServerConfig());
        confirmServerConfigButton = new ConsoleButton(I18N.binding(I18N.CONSOLE_BUTTON_CONFIRM), event -> confirmServerConfig());
        cancelServerConfigButton = new ConsoleButton(I18N.binding(I18N.CONSOLE_BUTTON_CANCEL), event -> hide());
        currentConfigHostTextField = new ConsoleTextField();
        currentConfigPortTextField = new NumericTextField();
        currentConfigPasswordPasswordField = new JFXPasswordField();
        currentConfigPasswordTextField = new ConsolePasswordTextField();
        currentConfigRemarkTextField = new ConsoleTextField();
        currentConfigPasswordToggleButton = new CurrentConfigPasswordToggleButton(event -> showCurrentConfigPassword());
        currentConfigCipherChoiceBox = new CurrentConfigCipherChoiceBox();
        currentConfigProtocolChoiceBox = new CurrentConfigProtocolChoiceBox();
        clientConfigPortTextField = new NumericTextField();
        requiredFieldValidator.messageProperty().bind(I18N.binding(I18N.CONSOLE_VALIDATOR_REQUIRED_FIELD_MESSAGE));
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

    private JFXTabPane initTabPane() {
        // ====================
        // tab0 gridPane0
        // ====================
        GridPane gridPane0 = new GridPane();
        // ----------- ColumnConstraints -----------
        // corner grid
        ColumnConstraints cConner = new ConsoleColumnConstraints(20);
        // gap grid
        ColumnConstraints cGap1 = new ConsoleColumnConstraints(10);
        // container grid
        ColumnConstraints cContainer0 = new ColumnConstraints();
        ObservableList<ColumnConstraints> cConstraints0 = gridPane0.getColumnConstraints();
        cConstraints0.add(cConner);
        for (int i = 0; i < 5; i++) {
            cConstraints0.add(cContainer0);
            cConstraints0.add(cGap1);
        }
        cConstraints0.add(cContainer0);
        cConstraints0.add(cConner);
        // ----------- RowConstraints -----------
        // corner grid
        RowConstraints rCorner = new ConsoleRowConstraints(20);
        // gap grid
        RowConstraints rGap0 = new ConsoleRowConstraints(20);
        RowConstraints rGap1 = new ConsoleRowConstraints(10);
        // container grid
        RowConstraints rContainer0 = new ConsoleRowConstraints(35);
        RowConstraints rContainer1 = new ConsoleRowConstraints(30);
        RowConstraints rContainer2 = new RowConstraints();
        rContainer2.setVgrow(Priority.ALWAYS);
        ObservableList<RowConstraints> rConstraints0 = gridPane0.getRowConstraints();
        rConstraints0.add(rCorner);
        for (int i = 0; i < 6; i++) {
            rConstraints0.add(rContainer0);
            rConstraints0.add(rGap0);
        }
        rConstraints0.add(rContainer0);
        for (int i = 0; i < 2; i++) {
            rConstraints0.add(rGap1);
            rConstraints0.add(rContainer1);
        }
        rConstraints0.add(rCorner);
        // grid children
        addGridPane0Children(gridPane0);
        // tab0
        Tab tab0 = new Tab();
        tab0.textProperty().bind(I18N.binding(I18N.CONSOLE_TAB0_TEXT));
        tab0.setContent(gridPane0);
        tab0.setClosable(false);
        // tab1
        Tab tab1 = newSingleNodeTab(logTextArea, I18N.binding(I18N.CONSOLE_TAB1_TEXT));
        // tab2
        tab2 = initTrafficTab();
        // ====================
        // main tab pane
        // ====================
        JFXTabPane tabPane = new JFXTabPane();
        tabPane.getTabs().addAll(tab0, tab1, tab2);
        tabPane.getStylesheets().add(Resource.CONSOLE_CSS.toExternalForm());
        return tabPane;
    }

    private Tab newSingleNodeTab(Node node, StringBinding tabTitle) {
        // ====================
        // tab gridPane
        // ====================
        GridPane gridPane = new GridPane();
        // ----------- GapConstraints -----------
        ColumnConstraints cGap = new ColumnConstraints(10);
        RowConstraints rGap = new RowConstraints(10);
        // ----------- NodeConstraints -----------
        ColumnConstraints cAlways = new ColumnConstraints();
        cAlways.setHgrow(Priority.ALWAYS);
        RowConstraints rAlways = new RowConstraints();
        rAlways.setVgrow(Priority.ALWAYS);
        // ----------- ColumnConstraints -----------
        ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
        columnConstraints.add(cGap);
        columnConstraints.add(cAlways);
        columnConstraints.add(cGap);
        // ----------- RowConstraints -----------
        ObservableList<RowConstraints> rowConstraints = gridPane.getRowConstraints();
        rowConstraints.add(rGap);
        rowConstraints.add(rAlways);
        rowConstraints.add(rGap);
        // grid children
        gridPane.add(node, 1, 1);
        // tab
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

    private void addGridPane0Children(GridPane gridPane0) {
        // ---------- Grid Children ----------
        gridPane0.add(wrap(moveUpServerConfigButton), 1, 13);
        gridPane0.add(wrap(moveDownServerConfigButton), 5, 13);
        gridPane0.add(newServerConfigButton, 1, 15);
        gridPane0.add(copyServerConfigButton, 3, 15);
        gridPane0.add(delServerConfigButton, 5, 15);
        gridPane0.add(importServerConfigButton, 1, 17);
        gridPane0.add(shareServerConfigButton, 3, 17);
        gridPane0.add(confirmServerConfigButton, 9, 17);
        gridPane0.add(cancelServerConfigButton, 11, 17);
        gridPane0.add(serverConfigJFXListView, 1, 1, 5, 11);
        gridPane0.add(new ConsoleLabel(I18N.binding(I18N.CONSOLE_LABEL_HOST)), 7, 1);
        gridPane0.add(new ConsoleLabel(I18N.binding(I18N.CONSOLE_LABEL_PORT)), 7, 3);
        gridPane0.add(new ConsoleLabel(I18N.binding(I18N.CONSOLE_LABEL_PASSWORD)), 7, 5);
        gridPane0.add(new ConsoleLabel(I18N.binding(I18N.CONSOLE_LABEL_CIPHER)), 7, 7);
        gridPane0.add(new ConsoleLabel(I18N.binding(I18N.CONSOLE_LABEL_PROTOCOL)), 7, 9);
        gridPane0.add(new ConsoleLabel(I18N.binding(I18N.CONSOLE_LABEL_REMARK)), 7, 11);
        gridPane0.add(new ConsoleLabel(I18N.binding(I18N.CONSOLE_LABEL_PROXY_PORT)), 7, 13);
        gridPane0.add(currentConfigHostTextField, 9, 1, 3, 1);
        gridPane0.add(currentConfigPortTextField, 9, 3, 3, 1);
        gridPane0.add(currentConfigPasswordTextField, 9, 5, 3, 1);
        gridPane0.add(currentConfigPasswordPasswordField, 9, 5, 3, 1);
        gridPane0.add(currentConfigPasswordToggleButton, 11, 5);
        gridPane0.add(currentConfigCipherChoiceBox, 9, 7, 3, 1);
        gridPane0.add(currentConfigProtocolChoiceBox, 9, 9, 3, 1);
        gridPane0.add(currentConfigRemarkTextField, 9, 11, 3, 1);
        gridPane0.add(clientConfigPortTextField, 9, 13, 3, 1);
    }

    private static HBox wrap(Node node) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.BOTTOM_CENTER);
        hbox.getChildren().add(node);
        return hbox;
    }

    private void initModule() {
        initWidget();
        root = initTabPane();
    }

    private void initController() {
        initServerConfigListView();
        initCurrentConfigCipherChoiceBox();
        initCurrentConfigProtocolChoiceBox();
        initCurrentConfigPortTextField();
        initCurrentConfigPasswordTextField();
        initCurrentConfigPasswordPasswordField();
        initClientConfigPortTextField();
        initShareServerConfigButton();
        initImportServerConfigButton();
        display();
    }

    private void initClientConfigPortTextField() {
        clientConfigPortTextField.getValidators().add(requiredFieldValidator);
        clientConfigPortTextField.textProperty().addListener((o, oldValue, newValue) -> {
            clientConfigPortTextField.validate();
            int port = clientConfigPortTextField.getIntValue();
            if (CLIENT_CONFIG.getPort() != port) {
                CLIENT_CONFIG.setPort(port);
                launchProxy();
                saveConfig();
            }
        });
    }

    private void initCurrentConfigPasswordPasswordField() {
        currentConfigPasswordPasswordField.getValidators().add(requiredFieldValidator);
        currentConfigPasswordPasswordField.focusedProperty().addListener((o, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(oldValue) && Boolean.FALSE.equals(newValue)) {
                currentConfigPasswordPasswordField.validate();
            }
        });
        initCurrentConfigPasswordCommonEvent(currentConfigPasswordPasswordField);
    }

    private void initCurrentConfigPasswordTextField() {
        currentConfigPasswordTextField.getValidators().add(requiredFieldValidator);
        currentConfigPasswordTextField.focusedProperty().addListener((o, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(oldValue) && Boolean.FALSE.equals(newValue)) {
                currentConfigPasswordTextField.validate();
            }
        });
        initCurrentConfigPasswordCommonEvent(currentConfigPasswordTextField);
    }

    private void initCurrentConfigPasswordCommonEvent(TextField field) {
        field.textProperty().addListener((o, oldValue, newValue) -> field.setText(newValue));
        field.setOnMouseEntered(event -> {
            if (field.isVisible()) {
                currentConfigPasswordToggleButton.setVisible(true);
            }
        });
        field.setOnMouseExited(event -> {
            if (field.isVisible()) {
                currentConfigPasswordToggleButton.setVisible(false);
            }
        });
    }

    private void initCurrentConfigPortTextField() {
        currentConfigPortTextField.getValidators().add(requiredFieldValidator);
        currentConfigPortTextField.textProperty().addListener((o, oldValue, newValue) -> currentConfigPortTextField.validate());
    }

    private void initCurrentConfigCipherChoiceBox() {
        List<CipherKind> ciphers = Arrays.asList(CipherKind.values());
        currentConfigCipherChoiceBox.setItems(FXCollections.observableArrayList(ciphers));
        currentConfigCipherChoiceBox.setValue(CipherKind.aes_128_gcm);
        currentConfigCipherChoiceBox.disableProperty().bind(Bindings.equal(Protocol.trojan, currentConfigProtocolChoiceBox.valueProperty()));
        // currentConfigHostTextField
        currentConfigHostTextField.getValidators().add(requiredFieldValidator);
        currentConfigHostTextField.textProperty().addListener((o, oldValue, newValue) -> currentConfigHostTextField.validate());
    }

    private void initCurrentConfigProtocolChoiceBox() {
        List<Protocol> ciphers = Arrays.asList(Protocol.values());
        currentConfigProtocolChoiceBox.setItems(FXCollections.observableArrayList(ciphers));
        currentConfigProtocolChoiceBox.setValue(Protocol.shadowsocks);
    }

    private void initShareServerConfigButton() {
        shareServerConfigButton.visibleProperty().bind(Bindings.equal(Protocol.shadowsocks, currentConfigProtocolChoiceBox.valueProperty()));
    }

    private void initImportServerConfigButton() {
        importServerConfigButton.visibleProperty().bind(Bindings.equal(Protocol.shadowsocks, currentConfigProtocolChoiceBox.valueProperty()));
    }

    private void initServerConfigListView() {
        serverConfigObservableList = FXCollections.observableArrayList(CLIENT_CONFIG.getServers());
        CLIENT_CONFIG.setServers(serverConfigObservableList);
        serverConfigJFXListView.setItems(serverConfigObservableList);
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigJFXListView.getSelectionModel();
        selectionModel.select(CLIENT_CONFIG.getIndex());
        selectionModel.selectedItemProperty().addListener(new ChangeListener<>() {

            private boolean changing = false;

            @Override
            public void changed(ObservableValue<? extends ServerConfig> observable, ServerConfig oldValue, ServerConfig newValue) {
                if (serverConfigObservableList.contains(oldValue)) {
                    if (validate()) {
                        resetValidation();
                        pack(oldValue);
                        serverConfigJFXListView.refresh();
                        display(newValue);
                    } else if (!changing) {
                        changing = true;
                        Platform.runLater(() -> {
                            selectionModel.select(oldValue);
                            changing = false;
                        });
                    }
                } else {
                    resetValidation();
                    display(newValue);
                }
            }
        });
    }

    private void shareServerConfig() {
        ShareableServerConfig.produceUri(serverConfigJFXListView.getSelectionModel().getSelectedItem()).ifPresent(uri -> {
            String string = uri.toString();
            TextInputDialog dialog = new TextInputDialog();
            dialog.setGraphic(null);
            dialog.titleProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_SHARE));
            dialog.setHeaderText(null);
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.lookupButton(ButtonType.OK).setVisible(false);
            dialogPane.lookupButton(ButtonType.CANCEL).setVisible(false);
            dialogPane.setPrefWidth((string.length() * 8));
            TextField editor = dialog.getEditor();
            editor.setText(string);
            editor.setEditable(false);
            dialog.show();
        });
    }

    private void importServerConfig() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setGraphic(null);
        dialog.titleProperty().bind(I18N.binding(I18N.CONSOLE_BUTTON_IMPORT));
        dialog.setHeaderText(null);
        dialog.showAndWait().map(URI::create).flatMap(ShareableServerConfig::fromUri).ifPresent(serverConfigObservableList::add);
    }

    private void initTrafficCounterLineChart() {
        ObservableList<Node> children = ((VBox) tab2.getContent()).getChildren();
        primaryStage.setOnHidden(event -> {
            children.clear();
            trafficCounterLineChartBackstage.stop();
        });
        primaryStage.setOnShown(event -> {
            if (children.isEmpty()) {
                ClientChannelTrafficTableView tableView = new ClientChannelTrafficTableView(channelTraffic);
                children.add(trafficCounterLineChartBackstage.newLineChart());
                children.add(tableView);
                VBox.setVgrow(tableView, Priority.SOMETIMES);
            }
            Optional.of(trafficCounter).map(ObservableObjectValue::get).ifPresent(trafficCounterLineChartBackstage::refresh);
        });
    }

    private boolean validate() {
        boolean result = currentConfigHostTextField.validate();
        result |= currentConfigPortTextField.validate();
        if (currentConfigPasswordTextField.isVisible()) {
            result &= currentConfigPasswordTextField.validate();
        }
        if (currentConfigPasswordPasswordField.isVisible()) {
            result &= currentConfigPasswordPasswordField.validate();
        }
        return result;
    }

    private void resetValidation() {
        currentConfigHostTextField.resetValidation();
        currentConfigPortTextField.resetValidation();
        currentConfigPasswordTextField.resetValidation();
        currentConfigPasswordPasswordField.resetValidation();
    }

    private void display() {
        clientConfigPortTextField.setText(Console.CLIENT_CONFIG.getPort());
        display(Console.CLIENT_CONFIG.getCurrent());
    }

    private void display(ServerConfig c) {
        if (c != null) {
            currentConfigHostTextField.setText(c.getHost());
            currentConfigPortTextField.setText(c.getPort());
            currentConfigRemarkTextField.setText(c.getRemark());
            String password = c.getPassword();
            currentConfigPasswordPasswordField.setText(password);
            currentConfigPasswordTextField.setText(password);
            currentConfigPasswordToggleButton.setSelected(false);
            currentConfigCipherChoiceBox.setValue(c.getCipher());
            currentConfigProtocolChoiceBox.setValue(c.getProtocol());
        }
    }

    private void pack(ServerConfig config) {
        config.setHost(currentConfigHostTextField.getText());
        config.setPort(currentConfigPortTextField.getIntValue());
        config.setPassword(currentConfigPasswordTextField.getText());
        config.setRemark(currentConfigRemarkTextField.getText());
        config.setCipher(currentConfigCipherChoiceBox.getValue());
        config.setProtocol(currentConfigProtocolChoiceBox.getValue());
    }

    private void saveConfig() {
        ConfigHandler.DEFAULT.save(CLIENT_CONFIG);
        tray.refresh();
    }

    public void launchProxy() {
        proxy.launch().ifPresent(instance -> {
            trafficCounter.set(instance.traffic());
            channelTraffic.set(instance.channelTraffic());
        });
    }
}
