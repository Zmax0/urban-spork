package com.urbanspork.client.gui.console;

import com.urbanspork.client.gui.Resource;
import com.urbanspork.client.gui.console.widget.ConsoleColumnConstraints;
import com.urbanspork.client.gui.console.widget.ConsoleLabel;
import com.urbanspork.client.gui.console.widget.ConsolePasswordField;
import com.urbanspork.client.gui.console.widget.ConsolePasswordTextField;
import com.urbanspork.client.gui.console.widget.ConsoleRowConstraints;
import com.urbanspork.client.gui.console.widget.ConsoleTextField;
import com.urbanspork.client.gui.console.widget.CurrentConfigPasswordToggleButton;
import com.urbanspork.client.gui.console.widget.NumericTextField;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocol;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.Optional;

import static com.urbanspork.client.gui.i18n.I18N.*;

final class ServerConfigDialog {
    private final Dialog<ServerConfig> dialog = new Dialog<>();
    private final ConsoleTextField currentConfigHostTextField = new ConsoleTextField();
    private final NumericTextField currentConfigPortTextField = new NumericTextField();
    private final ConsolePasswordField currentConfigPasswordPasswordField = new ConsolePasswordField();
    private final ConsolePasswordTextField currentConfigPasswordTextField = new ConsolePasswordTextField();
    private final ConsoleTextField currentConfigRemarkTextField = new ConsoleTextField();
    private final CurrentConfigPasswordToggleButton currentConfigPasswordToggleButton = new CurrentConfigPasswordToggleButton(_ -> showCurrentConfigPassword());
    private final ChoiceBox<CipherKind> currentConfigCipherChoiceBox = new ChoiceBox<>();
    private final ChoiceBox<Protocol> currentConfigProtocolChoiceBox = new ChoiceBox<>();
    private final ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    private ServerConfig workingCopy;

    ServerConfigDialog(Stage owner) {
        initWidget();
        initDialog(owner);
        initController();
    }

    Optional<ServerConfig> show(ServerConfig source) {
        workingCopy = source;
        display(source);
        return dialog.showAndWait();
    }

    private void initWidget() {
        currentConfigHostTextField.bindRequiredMessage(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_VALIDATOR_REQUIRED_FIELD_MESSAGE));
        currentConfigPortTextField.bindRequiredMessage(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_VALIDATOR_REQUIRED_FIELD_MESSAGE));
        currentConfigPasswordPasswordField.bindRequiredMessage(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_VALIDATOR_REQUIRED_FIELD_MESSAGE));
        currentConfigPasswordTextField.bindRequiredMessage(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_VALIDATOR_REQUIRED_FIELD_MESSAGE));
        currentConfigPasswordPasswordField.getStyleClass().add("password-toggle-input");
        currentConfigPasswordTextField.getStyleClass().add("password-toggle-input");
    }

    private void initDialog(Stage owner) {
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(true);
        dialog.titleProperty().bind(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_SERVER_DIALOG_TITLE));
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(Resource.CONSOLE_CSS.toExternalForm());
        pane.getButtonTypes().addAll(confirmButtonType, cancelButtonType);
        pane.contentProperty().set(buildContent());
        Button okButton = (Button) pane.lookupButton(confirmButtonType);
        Button cancelButton = (Button) pane.lookupButton(cancelButtonType);
        okButton.textProperty().bind(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_BUTTON_CONFIRM));
        cancelButton.textProperty().bind(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_BUTTON_CANCEL));
        okButton.disableProperty().bind(Bindings.createBooleanBinding(
            () -> !validate(),
            currentConfigHostTextField.textProperty(),
            currentConfigPortTextField.textProperty(),
            currentConfigPasswordTextField.textProperty(),
            currentConfigPasswordPasswordField.textProperty(),
            currentConfigPasswordPasswordField.visibleProperty(),
            currentConfigPasswordTextField.visibleProperty()
        ));
        dialog.setResultConverter(buttonType -> buttonType == confirmButtonType ? pack() : null);
    }

    private GridPane buildContent() {
        GridPane gridPane = new GridPane();
        gridPane.setVgap(12);
        ObservableList<ColumnConstraints> columns = gridPane.getColumnConstraints();
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setHalignment(HPos.RIGHT);
        ColumnConstraints cGap = new ConsoleColumnConstraints(10);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        labelColumn.setHalignment(HPos.LEFT);
        columns.addAll(cGap, labelColumn, cGap, fieldColumn, cGap);
        ObservableList<RowConstraints> rows = gridPane.getRowConstraints();
        for (int i = 0; i < 6; i++) {
            rows.add(new ConsoleRowConstraints(32));
        }
        gridPane.add(new ConsoleLabel(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_LABEL_HOST)), 1, 0);
        gridPane.add(new ConsoleLabel(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_LABEL_PORT)), 1, 1);
        gridPane.add(new ConsoleLabel(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_LABEL_PASSWORD)), 1, 2);
        gridPane.add(new ConsoleLabel(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_LABEL_CIPHER)), 1, 3);
        gridPane.add(new ConsoleLabel(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_LABEL_PROTOCOL)), 1, 4);
        gridPane.add(new ConsoleLabel(com.urbanspork.client.gui.i18n.I18N.binding(CONSOLE_LABEL_REMARK)), 1, 5);
        gridPane.add(currentConfigHostTextField, 3, 0);
        gridPane.add(currentConfigPortTextField, 3, 1);
        StackPane passwordPane = new StackPane(currentConfigPasswordTextField, currentConfigPasswordPasswordField, currentConfigPasswordToggleButton);
        StackPane.setAlignment(currentConfigPasswordToggleButton, Pos.CENTER_RIGHT);
        passwordPane.setOnMouseEntered(_ -> currentConfigPasswordToggleButton.setVisible(true));
        passwordPane.setOnMouseExited(_ -> currentConfigPasswordToggleButton.setVisible(false));
        gridPane.add(passwordPane, 3, 2);
        gridPane.add(currentConfigCipherChoiceBox, 3, 3);
        gridPane.add(currentConfigProtocolChoiceBox, 3, 4);
        gridPane.add(currentConfigRemarkTextField, 3, 5);
        return gridPane;
    }

    private void initController() {
        currentConfigHostTextField.textProperty().addListener((_, _, _) -> currentConfigHostTextField.validate());
        currentConfigPortTextField.textProperty().addListener((_, _, _) -> currentConfigPortTextField.validate());
        initCurrentConfigPasswordPasswordField();
        initCurrentConfigPasswordTextField();
        initCurrentConfigCipherChoiceBox();
        initCurrentConfigProtocolChoiceBox();
    }

    private void initCurrentConfigPasswordPasswordField() {
        currentConfigPasswordPasswordField.inputFocusedProperty().addListener((_, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(oldValue) && Boolean.FALSE.equals(newValue)) {
                currentConfigPasswordPasswordField.validate();
            }
        });
    }

    private void initCurrentConfigPasswordTextField() {
        currentConfigPasswordTextField.inputFocusedProperty().addListener((_, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(oldValue) && Boolean.FALSE.equals(newValue)) {
                currentConfigPasswordTextField.validate();
            }
        });
    }

    private void initCurrentConfigCipherChoiceBox() {
        currentConfigCipherChoiceBox.setItems(FXCollections.observableArrayList(Arrays.asList(CipherKind.values())));
        currentConfigCipherChoiceBox.setValue(CipherKind.aes_128_gcm);
        currentConfigCipherChoiceBox.disableProperty().bind(Bindings.equal(Protocol.trojan, currentConfigProtocolChoiceBox.valueProperty()));
    }

    private void initCurrentConfigProtocolChoiceBox() {
        currentConfigProtocolChoiceBox.setItems(FXCollections.observableArrayList(Arrays.asList(Protocol.values())));
        currentConfigProtocolChoiceBox.setValue(Protocol.shadowsocks);
    }

    private void showCurrentConfigPassword() {
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

    private boolean validate() {
        boolean result = currentConfigHostTextField.validate();
        result &= currentConfigPortTextField.validate();
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

    private void display(ServerConfig config) {
        resetValidation();
        currentConfigHostTextField.setText(config.getHost());
        currentConfigPortTextField.setText(config.getPort());
        currentConfigRemarkTextField.setText(config.getRemark());
        String password = config.getPassword();
        currentConfigPasswordPasswordField.setText(password);
        currentConfigPasswordTextField.setText(password);
        currentConfigPasswordToggleButton.setSelected(false);
        currentConfigPasswordToggleButton.setVisible(false);
        currentConfigPasswordPasswordField.setVisible(true);
        currentConfigPasswordTextField.setVisible(false);
        currentConfigCipherChoiceBox.setValue(config.getCipher() == null ? CipherKind.aes_256_gcm : config.getCipher());
        currentConfigProtocolChoiceBox.setValue(config.getProtocol() == null ? Protocol.shadowsocks : config.getProtocol());
    }

    private ServerConfig pack() {
        workingCopy.setHost(currentConfigHostTextField.getText());
        currentConfigPortTextField.getIntValue().ifPresent(workingCopy::setPort);
        workingCopy.setPassword(currentConfigPasswordTextField.isVisible() ? currentConfigPasswordTextField.getText() : currentConfigPasswordPasswordField.getText());
        workingCopy.setRemark(currentConfigRemarkTextField.getText());
        workingCopy.setCipher(currentConfigCipherChoiceBox.getValue());
        workingCopy.setProtocol(currentConfigProtocolChoiceBox.getValue());
        return workingCopy;
    }
}
