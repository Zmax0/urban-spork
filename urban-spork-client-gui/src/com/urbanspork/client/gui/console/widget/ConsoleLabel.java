package com.urbanspork.client.gui.console.widget;

import javafx.beans.binding.StringBinding;
import javafx.scene.control.Label;

public class ConsoleLabel extends Label {

    public ConsoleLabel(StringBinding textBinding) {
        textProperty().bind(textBinding);
    }

}
