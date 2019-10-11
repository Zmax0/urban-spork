package com.urbanspork.client.mvc.component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.RequiredFieldValidator;
import com.urbanspork.cipher.ShadowsocksCiphers;
import com.urbanspork.client.mvc.Components;
import com.urbanspork.client.mvc.Resource;
import com.urbanspork.client.mvc.component.element.ConsoleButton;
import com.urbanspork.client.mvc.component.element.ConsoleLabel;
import com.urbanspork.client.mvc.component.element.ConsoleLogTextArea;
import com.urbanspork.client.mvc.component.element.ConsolePasswordTextField;
import com.urbanspork.client.mvc.component.element.ConsoleTextField;
import com.urbanspork.client.mvc.component.element.CurrentConfigCipherChoiceBox;
import com.urbanspork.client.mvc.component.element.CurrentConfigPasswordToggleButton;
import com.urbanspork.client.mvc.component.element.ServerConfigListView;
import com.urbanspork.client.mvc.i18n.I18n;
import com.urbanspork.config.ClientConfig;
import com.urbanspork.config.ServerConfig;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Console extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Console.class);

    private final ClientConfig clientConfig = Resource.config();

    private Parent root;

    private Stage primaryStage;

    private ObservableList<ServerConfig> serverConfigObservableList;

    private RequiredFieldValidator requiredFieldValidator;

    private JFXListView<ServerConfig> serverConfigListView;

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

    private TextArea logTextArea;

    public static void launch(String... args) {
        Thread thread = new Thread(() -> {
            Application.launch(args);
        });
        thread.setName("Console-Launcher");
        thread.start();
    }

    @Override
    public void init() throws Exception {
        Components.register(this);
        initModule();
        initController();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        Platform.setImplicitExit(false);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.UNIFIED);
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
            }
        }
    }

    public void addServerConfig(ActionEvent event) {
        if (validate()) {
            ServerConfig newValue = new ServerConfig();
            newValue.setCipher(ShadowsocksCiphers.AES_256_CFB);
            serverConfigObservableList.add(newValue);
            serverConfigListView.getSelectionModel().select(newValue);
            display(newValue);
        }
    }

    public void deleteServerConfig(ActionEvent event) {
        int index = serverConfigListView.getSelectionModel().getSelectedIndex();
        if (index > 0) {
            serverConfigObservableList.remove(index);
            if (!serverConfigObservableList.isEmpty()) {
                serverConfigListView.getSelectionModel().select(index);
            }
        }
    }

    public void copyServerConfig(ActionEvent event) {
        ServerConfig config = serverConfigListView.getSelectionModel().getSelectedItem();
        if (config != null) {
            ServerConfig copyed = JSON.parseObject(JSON.toJSONString(config), ServerConfig.class);
            serverConfigObservableList.add(copyed);
        }
    }

    public void moveUpServerConfig(ActionEvent event) {
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigListView.getSelectionModel();
        int index = selectionModel.getSelectedIndex();
        if (index > 0) {
            ServerConfig config = serverConfigObservableList.get(index);
            serverConfigObservableList.remove(index);
            serverConfigObservableList.add(index - 1, config);
            selectionModel.select(index - 1);
        }
    }

    public void moveDownServerConfig(ActionEvent event) {
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigListView.getSelectionModel();
        int index = selectionModel.getSelectedIndex();
        if (index < serverConfigObservableList.size() - 1) {
            ServerConfig config = serverConfigObservableList.get(index);
            serverConfigObservableList.remove(index);
            serverConfigObservableList.add(index + 1, config);
            selectionModel.select(index + 1);
        }
    }

    public void showCurrentConfigPassword(ActionEvent event) {
        currentConfigPasswordPasswordField.visibleProperty().set(!currentConfigPasswordToggleButton.isSelected());
        currentConfigPasswordTextField.visibleProperty().set(currentConfigPasswordToggleButton.isSelected());
    }

    public void serverConfigListViewSelect(int index) {
        serverConfigListView.getSelectionModel().select(index);
    }

    public void confirm(ActionEvent event) {
        if (validate()) {
            MultipleSelectionModel<ServerConfig> selectionModel = serverConfigListView.getSelectionModel();
            ServerConfig config = selectionModel.getSelectedItem();
            boolean isNew = config == null;
            if (config == null) {
                config = new ServerConfig();
                config.setCipher(ShadowsocksCiphers.AES_256_CFB);
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

    public void cancel(ActionEvent event) {
        hideConsole();
        int lastIndex = serverConfigObservableList.size() - 1;
        if (lastIndex > -1) {
            ServerConfig lastConfig = serverConfigObservableList.get(lastIndex);
            if (!lastConfig.check()) {
                serverConfigObservableList.remove(lastIndex);
            }
            serverConfigListView.getSelectionModel().select(clientConfig.getCurrent());
        }
    }

    public TextArea getLogTextArea() {
        return logTextArea;
    }

    private void initModule() {
        initElement();
        // ==========
        // tab0 gridPane0
        // ==========
        GridPane gridPane0 = new GridPane();
        // ----------- ColumnConstraints -----------
        // corner grid
        ColumnConstraints ccConner = new ColumnConstraints();
        ccConner.setHgrow(Priority.SOMETIMES);
        ccConner.setMinWidth(20);
        ccConner.setMaxWidth(20);
        // gap grid
        ColumnConstraints ccGap1 = new ColumnConstraints();
        ccGap1.setHgrow(Priority.SOMETIMES);
        ccGap1.setMinWidth(10);
        ccGap1.setMaxWidth(10);
        ccGap1.setPrefWidth(10);
        ColumnConstraints ccGap2 = new ColumnConstraints();
        ccGap2.setHgrow(Priority.SOMETIMES);
        ccGap2.setMinWidth(10);
        ccGap2.setMaxWidth(20);
        ColumnConstraints ccGap3 = new ColumnConstraints();
        ccGap3.setHgrow(Priority.SOMETIMES);
        ccGap3.setMinWidth(50);
        ccGap3.setMaxWidth(100);
        // container grid
        ColumnConstraints ccContainer0 = new ColumnConstraints();
        ccContainer0.setHgrow(Priority.SOMETIMES);
        ccContainer0.setMinWidth(10);
        ccContainer0.setMaxWidth(100);
        ObservableList<ColumnConstraints> columnConstraints0 = gridPane0.getColumnConstraints();
        columnConstraints0.add(ccConner);
        columnConstraints0.add(ccContainer0);
        columnConstraints0.add(ccGap1);
        columnConstraints0.add(ccContainer0);
        columnConstraints0.add(ccGap1);
        columnConstraints0.add(ccContainer0);
        columnConstraints0.add(ccGap1);
        columnConstraints0.add(ccGap3);
        columnConstraints0.add(ccGap2);
        columnConstraints0.add(ccContainer0);
        columnConstraints0.add(ccGap2);
        columnConstraints0.add(ccContainer0);
        columnConstraints0.add(ccConner);
        // ----------- RowConstraints -----------
        // corner grid
        RowConstraints rcCorner = new RowConstraints();
        rcCorner.setVgrow(Priority.SOMETIMES);
        rcCorner.setMinHeight(20);
        rcCorner.setPrefHeight(20);
        // gap grid
        RowConstraints rcGap0 = new RowConstraints();
        rcGap0.setVgrow(Priority.SOMETIMES);
        rcGap0.setMinHeight(5);
        rcGap0.setPrefHeight(10);
        // container grid
        RowConstraints rcContainer0 = new RowConstraints();
        rcContainer0.setVgrow(Priority.SOMETIMES);
        rcContainer0.setMinHeight(10);
        rcContainer0.setPrefHeight(30);
        ObservableList<RowConstraints> rowConstraints0 = gridPane0.getRowConstraints();
        rowConstraints0.add(rcCorner);
        rowConstraints0.add(rcContainer0);
        rowConstraints0.add(rcGap0);
        rowConstraints0.add(rcContainer0);
        rowConstraints0.add(rcGap0);
        rowConstraints0.add(rcContainer0);
        rowConstraints0.add(rcGap0);
        rowConstraints0.add(rcContainer0);
        rowConstraints0.add(rcGap0);
        rowConstraints0.add(rcContainer0);
        rowConstraints0.add(rcGap0);
        rowConstraints0.add(rcContainer0);
        rowConstraints0.add(rcGap0);
        rowConstraints0.add(rcCorner);
        rowConstraints0.add(rcCorner);
        rowConstraints0.add(rcGap0);
        rowConstraints0.add(rcCorner);
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
        gridPane0.add(new ConsoleLabel(I18n.CONSOLE_LABEL_PROXY_PORT), 6, 13, 2, 1);
        gridPane0.add(currentConfigHostTextField, 9, 1, 3, 1);
        gridPane0.add(currentConfigPortTextField, 9, 3, 3, 1);
        gridPane0.add(currentConfigPasswordTextField, 9, 5, 3, 1);
        gridPane0.add(currentConfigPasswordPasswordField, 9, 5, 3, 1);
        gridPane0.add(currentConfigRemarkTextField, 9, 9, 3, 1);
        gridPane0.add(currentConfigPasswordToggleButton, 11, 5);
        gridPane0.add(currentConfigCipherChoiceBox, 9, 7, 3, 1);
        gridPane0.add(clientConfigPortTextField, 9, 13, 3, 1);
        Tab tab0 = new Tab(I18n.CONSOLE_TAB0_TEXT);
        tab0.setContent(gridPane0);
        // ==========
        // tab1 gridPane1
        // ==========
        Tab tab1 = new Tab(I18n.CONSOLE_TAB1_TEXT);
        GridPane gridPane1 = new GridPane();
        gridPane1.setPrefSize(440, 29);
        ObservableList<ColumnConstraints> columnConstraints1 = gridPane1.getColumnConstraints();
        ColumnConstraints ccContainer1 = new ColumnConstraints();
        ccContainer1.setHgrow(Priority.ALWAYS);
        columnConstraints1.add(ccGap1);
        columnConstraints1.add(ccContainer1);
        columnConstraints1.add(ccGap1);
        ObservableList<RowConstraints> rowConstraints1 = gridPane1.getRowConstraints();
        RowConstraints rcGap1 = new RowConstraints();
        rcGap1.setVgrow(Priority.SOMETIMES);
        rcGap1.setMinHeight(10);
        rcGap1.setPrefHeight(10);
        rcGap1.setMaxHeight(10);
        RowConstraints rcContainer1 = new RowConstraints();
        rcContainer1.setVgrow(Priority.ALWAYS);
        rowConstraints1.add(rcGap1);
        rowConstraints1.add(rcContainer1);
        rowConstraints1.add(rcGap1);
        gridPane1.add(logTextArea, 1, 1);
        tab1.setContent(gridPane1);
        // ==========
        // main tab pane
        // ==========
        TabPane tabPane = new TabPane(tab0, tab1);
        tabPane.setPrefSize(500, 500);
        tabPane.getStylesheets().add(Resource.CONSOLE_CSS.toExternalForm());
        root = tabPane;
    }

    private void initElement() {
        serverConfigListView = new ServerConfigListView();
        addServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_ADD, event -> addServerConfig(event));
        delServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_DEL, event -> deleteServerConfig(event));
        copyServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_COPY, event -> copyServerConfig(event));
        moveUpServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_UP, event -> moveUpServerConfig(event));
        moveDownServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_DOWN, event -> moveDownServerConfig(event));
        confirmServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_CONFIRM, event -> confirm(event));
        cancelServerConfigButton = new ConsoleButton(I18n.CONSOLE_BUTTON_CANCEL, event -> cancel(event));
        currentConfigHostTextField = new ConsoleTextField();
        currentConfigPortTextField = new ConsoleTextField();
        currentConfigPasswordPasswordField = new JFXPasswordField();
        currentConfigPasswordTextField = new ConsolePasswordTextField();
        currentConfigRemarkTextField = new ConsoleTextField();
        currentConfigPasswordToggleButton = new CurrentConfigPasswordToggleButton(event -> showCurrentConfigPassword(event));
        currentConfigCipherChoiceBox = new CurrentConfigCipherChoiceBox();
        clientConfigPortTextField = new ConsoleTextField();
        logTextArea = new ConsoleLogTextArea();
    }

    private void initController() {
        // requiredFieldValidator
        requiredFieldValidator = new RequiredFieldValidator(I18n.CONSOLE_VALIDATOR_REQUIRED_FIELD_MESSAGE);
        // serverConfigListView
        serverConfigObservableList = FXCollections.observableArrayList(clientConfig.getServers());
        clientConfig.setServers(serverConfigObservableList);
        serverConfigListView.setItems(serverConfigObservableList);
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigListView.getSelectionModel();
        selectionModel.select(clientConfig.getIndex());
        selectionModel.selectedItemProperty().addListener(
            new ChangeListener<ServerConfig>() {

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
        currentConfigCipherChoiceBox.setValue(ShadowsocksCiphers.AES_256_CFB);
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
            (o, oldValue, newValue) -> {
                currentConfigPasswordPasswordField.setText(newValue);
            });
        // currentConfigPasswordPasswordField
        currentConfigPasswordPasswordField.getValidators().add(requiredFieldValidator);
        currentConfigPasswordPasswordField.focusedProperty().addListener(
            (o, oldValue, newValue) -> {
                if (!newValue) {
                    currentConfigPasswordPasswordField.validate();
                }
            });
        currentConfigPasswordPasswordField.textProperty().addListener(
            (o, oldValue, newValue) -> {
                currentConfigPasswordTextField.setText(newValue);
            });
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

    private void hideConsole() {
        Components.CONSOLE.hide();
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
            clientConfigPortTextField.setText(c.getPort().toString());
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
        Components.TRAY.refresh();
    }

}
