package com.urbanspork.client.gui.console.widget;

import com.jfoenix.controls.JFXButton;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class ConsoleButton extends JFXButton {
    public ConsoleButton(StringBinding textBinding, EventHandler<ActionEvent> handler) {
        textProperty().bind(textBinding);
        setOnAction(handler);
    }
}
