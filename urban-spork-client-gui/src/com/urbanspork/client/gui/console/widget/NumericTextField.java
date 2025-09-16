package com.urbanspork.client.gui.console.widget;

import com.jfoenix.controls.JFXTextField;

import java.util.Optional;
import java.util.OptionalInt;

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

    public OptionalInt getIntValue() {
        return Optional.ofNullable(getText()).filter(s -> !s.isBlank()).stream().mapToInt(Integer::parseInt).findFirst();
    }

    public void setText(int value) {
        setText(Integer.toString(value));
    }
}
