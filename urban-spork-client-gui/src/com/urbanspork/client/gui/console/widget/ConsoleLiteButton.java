package com.urbanspork.client.gui.console.widget;

import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;


public class ConsoleLiteButton extends JFXButton {
    public ConsoleLiteButton(String text, EventHandler<ActionEvent> handler) {
        getStyleClass().add("lite-button");
        setText(text);
        setOnAction(handler);
    }
}
