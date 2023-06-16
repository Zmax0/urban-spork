package com.urbanspork.client.gui.console.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.RequiredFieldValidator;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.widget.*;
import com.urbanspork.client.gui.i18n.I18n;
import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Console extends Preloader {

    private static final Logger logger = LoggerFactory.getLogger(Console.class);

    private final ClientConfig clientConfig = Resource.config();

    private final RequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator(I18n.CONSOLE_VALIDATOR_REQUIRED_FIELD_MESSAGE);

    private Stage primaryStage;

    private TextArea logTextarea;

    private JFXListView<ServerConfig> serverConfigJFXListView;

    private Parent root;

    private ObservableList<ServerConfig> serverConfigObservableList;

    private Button addServerConfigButton;

    private Button delServerConfigButton;

    private Button copyServerConfigButton;

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

    private ChoiceBox<SupportedCipher> currentConfigCipherChoiceBox;

    private ChoiceBox<Protocols> currentConfigProtocolChoiceBox;

    private NumericTextField clientConfigPortTextField;

    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        if (info.getType().equals(StateChangeNotification.Type.BEFORE_START)) {
            Console console = (Console) info.getApplication();
            Appender.setConsole(console);
            Tray.init(console);
            Proxy.launch();
        }
    }

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
        primaryStage.setResizable(false);
        primaryStage.getIcons().add(new Image(Resource.PROGRAM_ICON.toString()));
        primaryStage.setTitle(I18n.PROGRAM_TITLE);
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            primaryStage.hide();
        });
        primaryStage.hide();
    }

    public void hide() {
        if (primaryStage != null) {
            primaryStage.hide();
        }
    }

    public void show() {
        if (primaryStage != null) {
            if (primaryStage.isIconified()) {
                primaryStage.setIconified(false);
            }
            if (!primaryStage.isShowing()) {
                primaryStage.show();
            } else {
                primaryStage.toFront();
            }
        }
    }

    public TextArea getLogTextArea() {
        return logTextarea;
    }

    public JFXListView<ServerConfig> getServerConfigJFXListView() {
        return serverConfigJFXListView;
    }

    public void addServerConfig() {
        if (validate()) {
            ServerConfig newValue = new ServerConfig();
            newValue.setCipher(SupportedCipher.aes_256_gcm);
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
                config.setCipher(SupportedCipher.aes_128_gcm);
            }
            pack(config);
            if (isNew) {
                serverConfigObservableList.add(config);
                serverConfigJFXListView.getSelectionModel().select(config);
            } else {
                serverConfigJFXListView.refresh();
            }
            clientConfig.setPort(clientConfigPortTextField.getIntValue());
            clientConfig.setIndex(selectionModel.getSelectedIndex());
            saveConfig();
            Proxy.launch();
        }
    }

    public void cancelServerConfig() {
        hide();
        int lastIndex = serverConfigObservableList.size() - 1;
        if (lastIndex > -1) {
            ServerConfig lastConfig = serverConfigObservableList.get(lastIndex);
            if (!lastConfig.check()) {
                serverConfigObservableList.remove(lastIndex);
            }
            serverConfigJFXListView.getSelectionModel().select(clientConfig.getCurrent());
        }
    }

    private void initElement() {
        serverConfigJFXListView = new ServerConfigListView();
        logTextarea = new ConsoleLogTextArea();
        addServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_ADD, event -> addServerConfig());
        delServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_DEL, event -> deleteServerConfig());
        copyServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_COPY, event -> copyServerConfig());
        moveUpServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_UP, event -> moveUpServerConfig());
        moveDownServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_DOWN, event -> moveDownServerConfig());
        confirmServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_CONFIRM, event -> confirmServerConfig());
        cancelServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_CANCEL, event -> cancelServerConfig());
        currentConfigHostTextField = new ConsoleTextField();
        currentConfigPortTextField = new NumericTextField();
        currentConfigPasswordPasswordField = new JFXPasswordField();
        currentConfigPasswordTextField = new ConsolePasswordTextField();
        currentConfigRemarkTextField = new ConsoleTextField();
        currentConfigPasswordToggleButton = new CurrentConfigPasswordToggleButton(event -> showCurrentConfigPassword());
        currentConfigCipherChoiceBox = new CurrentConfigCipherChoiceBox();
        currentConfigProtocolChoiceBox = new CurrentConfigProtocolChoiceBox();
        clientConfigPortTextField = new NumericTextField();
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
        RowConstraints rContainer1 = new RowConstraints();
        RowConstraints rContainer2 = new RowConstraints();
        rContainer2.setVgrow(Priority.ALWAYS);
        ObservableList<RowConstraints> rConstraints0 = gridPane0.getRowConstraints();
        rConstraints0.add(rCorner);
        for (int i = 0; i < 6; i++) {
            rConstraints0.add(rContainer0);
            rConstraints0.add(rGap0);
        }
        rConstraints0.add(rContainer0);
        rConstraints0.add(rGap1);
        for (int i = 0; i < 2; i++) {
            rConstraints0.add(rGap1);
            rConstraints0.add(rContainer1);
        }
        rConstraints0.add(rCorner);
        // grid children
        addGridPane0Children(gridPane0);
        // tab0
        Tab tab0 = new Tab(I18n.CONSOLE_TAB0_TEXT);
        tab0.setContent(gridPane0);
        tab0.setClosable(false);
        // ====================
        // tab1 gridPane1
        // ====================
        GridPane gridPane1 = new GridPane();
        // ----------- ColumnConstraints -----------
        ObservableList<ColumnConstraints> cConstraints1 = gridPane1.getColumnConstraints();
        ColumnConstraints cContainer2 = new ColumnConstraints();
        cContainer2.setHgrow(Priority.ALWAYS);
        cConstraints1.add(cGap1);
        cConstraints1.add(cContainer2);
        cConstraints1.add(cGap1);
        // ----------- RowConstraints -----------
        ObservableList<RowConstraints> rConstraints1 = gridPane1.getRowConstraints();
        rConstraints1.add(rGap1);
        rConstraints1.add(rContainer2);
        rConstraints1.add(rGap1);
        // grid children
        gridPane1.add(logTextarea, 1, 1);
        // tab1
        Tab tab1 = new Tab(I18n.CONSOLE_TAB1_TEXT);
        tab1.setContent(gridPane1);
        tab1.setClosable(false);
        // ====================
        // main tab pane
        // ====================
        JFXTabPane tabPane = new JFXTabPane();
        tabPane.getTabs().addAll(tab0, tab1);
        tabPane.getStylesheets().add(Resource.CONSOLE_CSS.toExternalForm());
        return tabPane;
    }

    private void addGridPane0Children(GridPane gridPane0) {
        // ---------- Grid Children ----------
        gridPane0.add(addServerConfigButton, 1, 13);
        gridPane0.add(copyServerConfigButton, 3, 13);
        gridPane0.add(moveUpServerConfigButton, 1, 15);
        gridPane0.add(moveDownServerConfigButton, 5, 15);
        gridPane0.add(delServerConfigButton, 5, 13);
        gridPane0.add(confirmServerConfigButton, 9, 15);
        gridPane0.add(cancelServerConfigButton, 11, 15);
        gridPane0.add(serverConfigJFXListView, 1, 1, 5, 11);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_HOST), 7, 1);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_PORT), 7, 3);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_PASSWORD), 7, 5);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_CIPHER), 7, 7);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_PROTOCOL), 7, 9);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_REMARK), 7, 11);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_PROXY_PORT), 7, 13);
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

    private void initModule() {
        initElement();
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
        display(clientConfig);
    }

    private void initClientConfigPortTextField() {
        clientConfigPortTextField.getValidators().add(requiredFieldValidator);
        clientConfigPortTextField.textProperty().addListener(
            (o, oldValue, newValue) -> {
                clientConfigPortTextField.validate();
                int port = clientConfigPortTextField.getIntValue();
                if (clientConfig.getPort() != port) {
                    clientConfig.setPort(port);
                    Proxy.launch();
                    saveConfig();
                }
            });
    }

    private void initCurrentConfigPasswordPasswordField() {
        currentConfigPasswordPasswordField.getValidators().add(requiredFieldValidator);
        currentConfigPasswordPasswordField.focusedProperty().addListener(
            (o, oldValue, newValue) -> {
                if (Boolean.TRUE.equals(oldValue) && Boolean.FALSE.equals(newValue)) {
                    currentConfigPasswordPasswordField.validate();
                }
            });
        initCurrentConfigPasswordCommonEvent(currentConfigPasswordPasswordField);
    }

    private void initCurrentConfigPasswordTextField() {
        currentConfigPasswordTextField.getValidators().add(requiredFieldValidator);
        currentConfigPasswordTextField.focusedProperty().addListener(
            (o, oldValue, newValue) -> {
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
        currentConfigPortTextField.textProperty().addListener(
            (o, oldValue, newValue) -> currentConfigPortTextField.validate());
    }

    private void initCurrentConfigCipherChoiceBox() {
        List<SupportedCipher> ciphers = Arrays.asList(SupportedCipher.values());
        currentConfigCipherChoiceBox.setItems(FXCollections.observableArrayList(ciphers));
        currentConfigCipherChoiceBox.setValue(SupportedCipher.aes_128_gcm);
        // currentConfigHostTextField
        currentConfigHostTextField.getValidators().add(requiredFieldValidator);
        currentConfigHostTextField.textProperty().addListener(
            (o, oldValue, newValue) -> currentConfigHostTextField.validate());
    }

    private void initCurrentConfigProtocolChoiceBox() {
        List<Protocols> ciphers = Arrays.asList(Protocols.values());
        currentConfigProtocolChoiceBox.setItems(FXCollections.observableArrayList(ciphers));
        currentConfigProtocolChoiceBox.setValue(Protocols.shadowsocks);
    }

    private void initServerConfigListView() {
        serverConfigObservableList = FXCollections.observableArrayList(clientConfig.getServers());
        clientConfig.setServers(serverConfigObservableList);
        serverConfigJFXListView.setItems(serverConfigObservableList);
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigJFXListView.getSelectionModel();
        selectionModel.select(clientConfig.getIndex());
        selectionModel.selectedItemProperty().addListener(
            new ChangeListener<>() {

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

    private void display(ClientConfig c) {
        clientConfigPortTextField.setText(c.getPort());
        display(c.getCurrent());
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
        ConfigHandler.DEFAULT.save(clientConfig);
        Tray.refresh();
    }

}
