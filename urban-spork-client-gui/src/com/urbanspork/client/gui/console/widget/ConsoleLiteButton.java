package com.urbanspork.client.gui.console.widget;

import com.jfoenix.controls.JFXButton;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;


public class ConsoleLiteButton extends JFXButton {
    public ConsoleLiteButton(StringBinding textBinding, EventHandler<ActionEvent> handler) {
        getStyleClass().add("lite-button");
        textProperty().bind(textBinding);
        setOnAction(handler);
    }
}
