package com.urbanspork.client.gui.console.widget;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;

public class ConsoleColumnConstraints extends ColumnConstraints {

    public ConsoleColumnConstraints(double width) {
        setHgrow(Priority.ALWAYS);
        setMinWidth(width);
    }

}
