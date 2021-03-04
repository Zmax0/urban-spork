package com.urbanspork.client.gui.console.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.RequiredFieldValidator;
import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.unit.ConsoleButton;
import com.urbanspork.client.gui.console.unit.ConsoleColumnConstraints;
import com.urbanspork.client.gui.console.unit.ConsoleLabel;
import com.urbanspork.client.gui.console.unit.ConsoleLogTextArea;
import com.urbanspork.client.gui.console.unit.ConsolePasswordTextField;
import com.urbanspork.client.gui.console.unit.ConsoleRowConstraints;
import com.urbanspork.client.gui.console.unit.ConsoleTextField;
import com.urbanspork.client.gui.console.unit.CurrentConfigCipherChoiceBox;
import com.urbanspork.client.gui.console.unit.CurrentConfigPasswordToggleButton;
import com.urbanspork.client.gui.console.unit.ServerConfigListView;
import com.urbanspork.client.gui.i18n.I18n;
import com.urbanspork.common.cipher.ShadowsocksCiphers;
import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ServerConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Console extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Console.class);

    private final ClientConfig clientConfig = Resource.config();

    private static Stage primaryStage;

    private static TextArea logTextArea;

    private static JFXListView<ServerConfig> serverConfigListView;

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

    private JFXTextField currentConfigPortTextField;

    private JFXPasswordField currentConfigPasswordPasswordField;

    private JFXTextField currentConfigPasswordTextField;

    private JFXTextField currentConfigRemarkTextField;

    private ToggleButton currentConfigPasswordToggleButton;

    private ChoiceBox<ShadowsocksCiphers> currentConfigCipherChoiceBox;

    private JFXTextField clientConfigPortTextField;

    public static void launch(String... args) {
        Application.launch(args);
    }

    @Override
    public void init() {
        initModule();
        initController();
    }

    @Override
    public void start(Stage primaryStage) {
        Proxy.launch();
        Console.primaryStage = primaryStage;
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

    public static void show() {
        if (primaryStage != null) {
            if (primaryStage.isIconified()) {
                primaryStage.setIconified(false);
            }
            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
        }
    }

    public static TextArea getLogTextArea() {
        return logTextArea;
    }

    public static JFXListView<ServerConfig> getServerConfigListView() {
        return serverConfigListView;
    }

    public void addServerConfig() {
        if (validate()) {
            ServerConfig newValue = new ServerConfig();
            newValue.setCipher(ShadowsocksCiphers.AES_256_GCM);
            serverConfigObservableList.add(newValue);
            serverConfigListView.getSelectionModel().select(newValue);
            display(newValue);
        }
    }

    public void deleteServerConfig() {
        int index = serverConfigListView.getSelectionModel().getSelectedIndex();
        if (index > 0) {
            serverConfigObservableList.remove(index);
            if (!serverConfigObservableList.isEmpty()) {
                serverConfigListView.getSelectionModel().select(index);
            }
        }
    }

    public void copyServerConfig() {
        ServerConfig config = serverConfigListView.getSelectionModel().getSelectedItem();
        if (config != null) {
            ObjectMapper mapper = new ObjectMapper();
            ServerConfig copyed;
            try {
                copyed = mapper.readValue(mapper.writeValueAsBytes(config), ServerConfig.class);
                serverConfigObservableList.add(copyed);
            } catch (IOException e) {
                logger.error("Copy server config error", e);
            }
        }
    }

    public void moveUpServerConfig() {
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigListView.getSelectionModel();
        int index = selectionModel.getSelectedIndex();
        if (index > 0) {
            ServerConfig config = serverConfigObservableList.get(index);
            serverConfigObservableList.remove(index);
            serverConfigObservableList.add(index - 1, config);
            selectionModel.select(index - 1);
        }
    }

    public void moveDownServerConfig() {
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigListView.getSelectionModel();
        int index = selectionModel.getSelectedIndex();
        if (index < serverConfigObservableList.size() - 1) {
            ServerConfig config = serverConfigObservableList.get(index);
            serverConfigObservableList.remove(index);
            serverConfigObservableList.add(index + 1, config);
            selectionModel.select(index + 1);
        }
    }

    public void showCurrentConfigPassword() {
        currentConfigPasswordPasswordField.visibleProperty().set(!currentConfigPasswordToggleButton.isSelected());
        currentConfigPasswordTextField.visibleProperty().set(currentConfigPasswordToggleButton.isSelected());
    }

    public void confirmServerConfig() {
        if (validate()) {
            MultipleSelectionModel<ServerConfig> selectionModel = serverConfigListView.getSelectionModel();
            ServerConfig config = selectionModel.getSelectedItem();
            boolean isNew = config == null;
            if (config == null) {
                config = new ServerConfig();
                config.setCipher(ShadowsocksCiphers.AES_256_GCM);
            }
            pack(config);
            if (isNew) {
                serverConfigObservableList.add(config);
                serverConfigListView.getSelectionModel().select(config);
            } else {
                serverConfigListView.refresh();
            }
            clientConfig.setPort(clientConfigPortTextField.getText());
            clientConfig.setIndex(selectionModel.getSelectedIndex());
            saveConfig();
            Proxy.relaunch();
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
            serverConfigListView.getSelectionModel().select(clientConfig.getCurrent());
        }
    }

    private void initElement() {
        serverConfigListView = new ServerConfigListView();
        logTextArea = new ConsoleLogTextArea();
        addServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_ADD, event -> addServerConfig());
        delServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_DEL, event -> deleteServerConfig());
        copyServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_COPY, event -> copyServerConfig());
        moveUpServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_UP, event -> moveUpServerConfig());
        moveDownServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_DOWN, event -> moveDownServerConfig());
        confirmServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_CONFIRM, event -> confirmServerConfig());
        cancelServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_CANCEL, event -> cancelServerConfig());
        currentConfigHostTextField = new ConsoleTextField();
        currentConfigPortTextField = new ConsoleTextField();
        currentConfigPasswordPasswordField = new JFXPasswordField();
        currentConfigPasswordTextField = new ConsolePasswordTextField();
        currentConfigRemarkTextField = new ConsoleTextField();
        currentConfigPasswordToggleButton = new CurrentConfigPasswordToggleButton(event -> showCurrentConfigPassword());
        currentConfigCipherChoiceBox = new CurrentConfigCipherChoiceBox();
        clientConfigPortTextField = new ConsoleTextField();
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
        gridPane1.add(logTextArea, 1, 1);
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
        gridPane0.add(serverConfigListView, 1, 1, 5, 11);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_HOST), 7, 1);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_PORT), 7, 3);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_PASSWORD), 7, 5);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_CIPHER), 7, 7);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_REMARK), 7, 9);
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_PROXY_PORT), 7, 13);
        gridPane0.add(currentConfigHostTextField, 9, 1, 3, 1);
        gridPane0.add(currentConfigPortTextField, 9, 3, 3, 1);
        gridPane0.add(currentConfigPasswordTextField, 9, 5, 3, 1);
        gridPane0.add(currentConfigPasswordPasswordField, 9, 5, 3, 1);
        gridPane0.add(currentConfigRemarkTextField, 9, 9, 3, 1);
        gridPane0.add(currentConfigPasswordToggleButton, 11, 5);
        gridPane0.add(currentConfigCipherChoiceBox, 9, 7, 3, 1);
        gridPane0.add(clientConfigPortTextField, 9, 13, 3, 1);
    }

    private void initModule() {
        initElement();
        root = initTabPane();
    }

    private void initController() {
        // requiredFieldValidator
        RequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator(I18n.CONSOLE_VALIDATOR_REQUIRED_FIELD_MESSAGE);
        // serverConfigListView
        serverConfigObservableList = FXCollections.observableArrayList(clientConfig.getServers());
        clientConfig.setServers(serverConfigObservableList);
        serverConfigListView.setItems(serverConfigObservableList);
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigListView.getSelectionModel();
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
                            serverConfigListView.refresh();
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
        // currentConfigCipherChoiceBox
        List<ShadowsocksCiphers> ciphers = Arrays.asList(ShadowsocksCiphers.values());
        currentConfigCipherChoiceBox.setItems(FXCollections.observableArrayList(ciphers));
        currentConfigCipherChoiceBox.setValue(ShadowsocksCiphers.AES_256_GCM);
        // currentConfigHostTextField
        currentConfigHostTextField.getValidators().add(requiredFieldValidator);
        currentConfigHostTextField.focusedProperty().addListener(
            (o, oldValue, newValue) -> {
                if (!newValue) {
                    currentConfigHostTextField.validate();
                }
            });
        // currentConfigPortTextField
        currentConfigPortTextField.getValidators().add(requiredFieldValidator);
        currentConfigPortTextField.focusedProperty().addListener(
            (o, oldValue, newValue) -> {
                if (!newValue) {
                    currentConfigPortTextField.validate();
                }
            });
        // currentConfigPasswordTextField
        currentConfigPasswordTextField.getValidators().add(requiredFieldValidator);
        currentConfigPasswordTextField.focusedProperty().addListener(
            (o, oldValue, newValue) -> {
                if (!newValue) {
                    currentConfigPasswordTextField.validate();
                }
            });
        currentConfigPasswordTextField.textProperty().addListener(
            (o, oldValue, newValue) -> currentConfigPasswordPasswordField.setText(newValue));
        // currentConfigPasswordPasswordField
        currentConfigPasswordPasswordField.getValidators().add(requiredFieldValidator);
        currentConfigPasswordPasswordField.focusedProperty().addListener(
            (o, oldValue, newValue) -> {
                if (!newValue) {
                    currentConfigPasswordPasswordField.validate();
                }
            });
        currentConfigPasswordPasswordField.textProperty().addListener(
            (o, oldValue, newValue) -> currentConfigPasswordTextField.setText(newValue));
        // clientConfigPortTextField
        clientConfigPortTextField.getValidators().add(requiredFieldValidator);
        clientConfigPortTextField.focusedProperty().addListener(
            (o, oldValue, newValue) -> {
                if (!newValue) {
                    clientConfigPortTextField.validate();
                    if (!clientConfig.getPort().equals(clientConfigPortTextField.getText())) {
                        clientConfig.setPort(clientConfigPortTextField.getText());
                        Proxy.relaunch();
                        saveConfig();
                    }
                }
            });
        // display file
        display(clientConfig);
    }

    private boolean validate() {
        boolean result = currentConfigHostTextField.validate() & currentConfigPortTextField.validate();
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
        if (c.getPort() != null) {
            clientConfigPortTextField.setText(c.getPort());
        }
        display(c.getCurrent());
    }

    private void display(ServerConfig c) {
        if (c != null) {
            currentConfigHostTextField.setText(c.getHost());
            currentConfigPortTextField.setText(c.getPort());
            currentConfigRemarkTextField.setText(c.getRemark());
            currentConfigPasswordPasswordField.setText(c.getPassword());
            currentConfigPasswordTextField.setText(c.getPassword());
            currentConfigPasswordToggleButton.setSelected(false);
            currentConfigCipherChoiceBox.setValue(c.getCipher());
        }
    }

    private void pack(ServerConfig config) {
        config.setHost(currentConfigHostTextField.getText());
        config.setPort(currentConfigPortTextField.getText());
        config.setPassword(currentConfigPasswordTextField.getText());
        config.setRemark(currentConfigRemarkTextField.getText());
        config.setCipher(currentConfigCipherChoiceBox.getValue());
    }

    private void saveConfig() {
        try {
            clientConfig.save();
        } catch (IOException e) {
            logger.error("Saving file failed", e);
            return;
        }
        Tray.refresh();
    }

}
