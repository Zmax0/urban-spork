package com.urbanspork.client.gui.console.widget;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;

import javafx.scene.control.TextFormatter;

public class NumericTextField extends ConsoleTextField {

    public NumericTextField() {
        UnaryOperator<TextFormatter.Change> filter = change -> change.getControlNewText().matches("\\d*") ? change : null;
        setTextFormatter(new TextFormatter<>(filter));
        textProperty().addListener((_, _, newValue) -> {
            if (newValue.isBlank()) {
                validate();
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
