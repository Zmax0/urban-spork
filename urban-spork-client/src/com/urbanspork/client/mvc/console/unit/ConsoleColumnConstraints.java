package com.urbanspork.client.mvc.console.unit;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;

public class ConsoleColumnConstraints extends ColumnConstraints {

    public ConsoleColumnConstraints() {

    }

    public ConsoleColumnConstraints(double width) {
        setHgrow(Priority.NEVER);
        setMinWidth(width);
    }

}
