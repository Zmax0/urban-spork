package com.urbanspork.client.gui.console.widget;

import com.jfoenix.controls.JFXToggleButton;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.GridPane;

public class CurrentConfigPasswordToggleButton extends JFXToggleButton {

    public CurrentConfigPasswordToggleButton(EventHandler<ActionEvent> value) {
        getStyleClass().add("hide-show");
        setSize(0);
        setOnAction(value);
        GridPane.setHalignment(this, HPos.RIGHT);
        GridPane.setValignment(this, VPos.CENTER);
        setVisible(false);
        setOnMouseEntered(event -> setVisible(true));
        setOnMouseExited(event -> setVisible(false));
    }

}
