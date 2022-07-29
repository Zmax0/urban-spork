package com.urbanspork.client.gui.console.unit;

import com.jfoenix.controls.JFXButton;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class ConsoleButton extends JFXButton {

    public ConsoleButton(String text, EventHandler<ActionEvent> handler) {
        setText(text);
        setOnAction(handler);
    }

}
