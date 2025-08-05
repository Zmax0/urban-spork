package com.urbanspork.client.gui.console.widget;

import com.jfoenix.controls.JFXTextField;

public class NumericTextField extends JFXTextField {

    public NumericTextField() {
        textProperty().addListener((_, _, newValue) -> {
            if (newValue.isBlank()) {
                validate();
            }
            if (!newValue.matches("\\d*")) {
                setText(newValue.replaceAll("\\D", ""));
            }
        });
    }

    public int getIntValue() {
        return Integer.parseInt(getText());
    }

    public void setText(int value) {
        setText(Integer.toString(value));
    }
}
