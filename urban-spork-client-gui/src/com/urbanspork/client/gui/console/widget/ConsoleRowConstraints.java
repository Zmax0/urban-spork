package com.urbanspork.client.gui.console.widget;

import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

public class ConsoleRowConstraints extends RowConstraints {

    public ConsoleRowConstraints(double height) {
        setVgrow(Priority.ALWAYS);
        setMinHeight(height);
    }

}
