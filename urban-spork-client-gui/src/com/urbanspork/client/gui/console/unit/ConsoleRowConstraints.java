package com.urbanspork.client.gui.console.unit;

import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

public class ConsoleRowConstraints extends RowConstraints {

    public ConsoleRowConstraints() {

    }

    public ConsoleRowConstraints(double height) {
        setVgrow(Priority.NEVER);
        setMinHeight(height);
    }

}
