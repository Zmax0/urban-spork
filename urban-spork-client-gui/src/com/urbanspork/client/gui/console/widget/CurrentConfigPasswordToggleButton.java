package com.urbanspork.client.gui.console.widget;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ToggleButton;

public class CurrentConfigPasswordToggleButton extends ToggleButton {

    public CurrentConfigPasswordToggleButton(EventHandler<ActionEvent> value) {
        getStyleClass().add("hide-show");
        setOnAction(value);
        setVisible(false);
    }

}
