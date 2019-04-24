package com.urbanspork.client.mvc.component;

import java.awt.TrayIcon.MessageType;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;
import com.jfoenix.validation.RequiredFieldValidator;
import com.urbanspork.cipher.ShadowsocksCiphers;
import com.urbanspork.client.Client;
import com.urbanspork.client.mvc.Component;
import com.urbanspork.config.ClientConfig;
import com.urbanspork.config.ConfigHandler;
import com.urbanspork.config.ServerConfig;

import io.netty.util.internal.StringUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MultipleSelectionModel;

public class Controller implements Initializable {

    private final Logger logger = LoggerFactory.getLogger(Controller.class);

    private ClientConfig clientConfig;

    private Thread clinetLauncher;

    private ObservableList<ServerConfig> serverConfigObservableList;

    private RequiredFieldValidator requiredFieldValidator;

    @FXML
    private JFXListView<ServerConfig> serverConfigListView;
    @FXML
    private JFXButton addServerConfigButton;
    @FXML
    private JFXButton delServerConfigButton;
    @FXML
    private JFXButton copyServerConfigButton;
    @FXML
    private JFXButton moveUpServerConfigButton;
    @FXML
    private JFXButton moveDownServerConfigButton;
    @FXML
    private JFXButton confirmServerConfigButton;
    @FXML
    private JFXButton cancelServerConfigButton;
    @FXML
    private JFXTextField currentConfigHostTextField;
    @FXML
    private JFXTextField currentConfigPortTextField;
    @FXML
    private JFXPasswordField currentConfigPasswordPasswordField;
    @FXML
    private JFXTextField currentConfigPasswordTextField;
    @FXML
    private JFXTextField currentConfigMemoTextField;
    @FXML
    private JFXToggleButton currentConfigPasswordToggleButton;
    @FXML
    private ChoiceBox<ShadowsocksCiphers> currentConfigCipherChoiceBox;
    @FXML
    private JFXTextField clientConfigPortTextField;
    @FXML
    private JFXTextArea logTextArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Component.Controller.set(this);
        loadConfig();
        initViews();
        launchClient();
    }

    @FXML
    public void addServerConfig(ActionEvent event) {
        ServerConfig config = new ServerConfig();
        serverConfigObservableList.add(config);
        serverConfigListView.getSelectionModel().select(config);
        display(config);
    }

    @FXML
    public void deleteServerConfig(ActionEvent event) {
        int index = serverConfigListView.getSelectionModel().getSelectedIndex();
        serverConfigObservableList.remove(index);
        if (!serverConfigObservableList.isEmpty()) {
            serverConfigListView.getSelectionModel().select(index);
        }
        saveConfig();
    }

    @FXML
    public void copyServerConfig(ActionEvent event) {
        ServerConfig config = serverConfigListView.getSelectionModel().getSelectedItem();
        if (config != null) {
            ServerConfig copyed = JSON.parseObject(JSON.toJSONString(config), ServerConfig.class);
            serverConfigObservableList.add(copyed);
            saveConfig();
        }
    }

    @FXML
    public void moveUpServerConfig(ActionEvent event) {
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigListView.getSelectionModel();
        int index = selectionModel.getSelectedIndex();
        if (index > 0) {
            ServerConfig config = serverConfigObservableList.get(index);
            serverConfigObservableList.remove(index);
            serverConfigObservableList.add(index - 1, config);
            selectionModel.select(index - 1);
        }
        saveConfig();
    }

    @FXML
    public void moveDownServerConfig(ActionEvent event) {
        MultipleSelectionModel<ServerConfig> selectionModel = serverConfigListView.getSelectionModel();
        int index = selectionModel.getSelectedIndex();
        if (index < serverConfigObservableList.size() - 1) {
            ServerConfig config = serverConfigObservableList.get(index);
            serverConfigObservableList.remove(index);
            serverConfigObservableList.add(index + 1, config);
            selectionModel.select(index + 1);
        }
        saveConfig();
    }

    @FXML
    public void showCurrentConfigPassword(ActionEvent event) {
        currentConfigPasswordPasswordField.visibleProperty().set(!currentConfigPasswordToggleButton.isSelected());
        currentConfigPasswordTextField.visibleProperty().set(currentConfigPasswordToggleButton.isSelected());
    }

    @FXML
    public void confirm(ActionEvent event) {
        ServerConfig config = serverConfigListView.getSelectionModel().getSelectedItem();
        if (config != clientConfig.getCurrent() && validate()) {
            config.setHost(currentConfigHostTextField.getText());
            config.setPort(currentConfigPortTextField.getText());
            config.setPassword(currentConfigPasswordTextField.getText());
            config.setMemo(currentConfigMemoTextField.getText());
            config.setCipher(currentConfigCipherChoiceBox.getValue());
            clientConfig.setPort(clientConfigPortTextField.getText());
            clientConfig.setCurrent(config);
            serverConfigListView.refresh();
            saveConfig();
            relaunchClient();
        }
    }

    @FXML
    public void cancel(ActionEvent event) {
        serverConfigListView.getSelectionModel().select(clientConfig.getCurrent());
        hideConsole();
    }

    public JFXTextArea getLogTextArea() {
        return logTextArea;
    }

    private void loadConfig() {
        try {
            clientConfig = ConfigHandler.read(ClientConfig.class);
        } catch (IOException e) {
            logger.error("Loading config failed", e);
        }
        if (clientConfig == null) {
            clientConfig = new ClientConfig();
            clientConfig.setServers(new ArrayList<>(16));
        }
    }

    private void initViews() {
        // requiredFieldValidator
        requiredFieldValidator = new RequiredFieldValidator("Can't be blank");
        // serverConfigListView
        serverConfigObservableList = FXCollections.observableArrayList(clientConfig.getServers());
        clientConfig.setServers(serverConfigObservableList);
        serverConfigListView.setItems(serverConfigObservableList);
        serverConfigListView.getSelectionModel().selectedItemProperty().addListener(
            (o, oldValue, newValue) -> {
                resetValidation();
                display(newValue);
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
                    if (clinetLauncher != null && !clientConfig.getPort().equals(clientConfigPortTextField.getText())) {
                        clientConfig.setPort(clientConfigPortTextField.getText());
                        relaunchClient();
                        saveConfig();
                    }
                }
            });
        // display config
        display(clientConfig);
    }

    private void relaunchClient() {
        if (clinetLauncher != null) {
            clinetLauncher.interrupt();
        }
        launchClient();
    }

    private void launchClient() {
        Tray tray = Component.Tray.get();
        ServerConfig config = clientConfig.getCurrent();
        if (config != null) {
            clinetLauncher = new Thread(() -> {
                try {
                    Client.launch(clientConfig);
                } catch (InterruptedException e) {
                    logger.info("[{}] was interrupted by relaunch", Thread.currentThread().getName());
                } catch (Exception e) {
                    logger.error(StringUtil.EMPTY_STRING, e);
                }
            });
            clinetLauncher.setName("Client-Launcher");
            clinetLauncher.setDaemon(true);
            clinetLauncher.start();
            tray.displayMessage("Proxy is running", clientConfig.getCurrent().toString(), MessageType.INFO);
        } else {
            tray.displayMessage("Proxy is not running", "Please set up a proxy server first", MessageType.INFO);
        }
    }

    private void hideConsole() {
        Console console = Component.Console.get();
        console.hide();
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
            currentConfigMemoTextField.setText(c.getMemo());
            currentConfigPasswordPasswordField.setText(c.getPassword());
            currentConfigPasswordTextField.setText(c.getPassword());
            currentConfigPasswordToggleButton.setSelected(false);
            currentConfigCipherChoiceBox.setValue(c.getCipher());
        }
    }

    private void saveConfig() {
        try {
            ConfigHandler.write(clientConfig);
        } catch (IOException e) {
            logger.error("Saving config failed", e);
        }
    }

}
