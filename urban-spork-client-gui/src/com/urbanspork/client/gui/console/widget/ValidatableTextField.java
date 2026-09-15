package com.urbanspork.client.gui.console.widget;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.StackPane;

public abstract class ValidatableTextField<T extends TextField> extends StackPane {
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

    private final T textField;
    private final Label label = new Label();
    private final StringProperty requiredMessage = new SimpleStringProperty("");

    protected ValidatableTextField(T textField) {
        this.textField = textField;
        getStyleClass().add("text-input-validation");
        setMaxWidth(Double.MAX_VALUE);
        textField.getStyleClass().add("console-text-field");
        textField.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("text-input-validation-label");
        label.setManaged(false);
        label.setVisible(false);
        label.setTranslateY(28);
        StackPane.setAlignment(label, Pos.TOP_LEFT);
        getChildren().addAll(textField, label);
        requiredMessage.addListener((_, _, newValue) -> {
            if (label.isVisible()) {
                label.setText(newValue);
            }
        });
    }

    public StringProperty textProperty() {
        return textField.textProperty();
    }

    public ReadOnlyBooleanProperty inputFocusedProperty() {
        return textField.focusedProperty();
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public void setTextFormatter(TextFormatter<?> textFormatter) {
        textField.setTextFormatter(textFormatter);
    }

    public boolean validate() {
        String text = textField.getText();
        boolean valid = text != null && !text.isBlank();
        textField.pseudoClassStateChanged(INVALID, !valid);
        pseudoClassStateChanged(INVALID, !valid);
        label.setText(valid ? "" : requiredMessage.get());
        label.setManaged(!valid);
        label.setVisible(!valid);
        return valid;
    }

    public void resetValidation() {
        textField.pseudoClassStateChanged(INVALID, false);
        pseudoClassStateChanged(INVALID, false);
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
    }

    public void bindRequiredMessage(StringBinding binding) {
        requiredMessage.bind(binding);
    }

    @Override
    public void requestFocus() {
        textField.requestFocus();
    }
}
