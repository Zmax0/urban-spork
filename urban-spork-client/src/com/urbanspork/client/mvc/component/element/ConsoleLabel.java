package com.urbanspork.client.mvc.component.element;

import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class ConsoleLabel extends Label {

    public ConsoleLabel(String text) {
        setText(text);
        GridPane.setHalignment(this, HPos.RIGHT);
    }

}
