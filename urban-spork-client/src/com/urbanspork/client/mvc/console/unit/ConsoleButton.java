package com.urbanspork.client.mvc.console.unit;

import com.jfoenix.controls.JFXButton;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;

public class ConsoleButton extends JFXButton {

    public ConsoleButton(String text, EventHandler<ActionEvent> handler) {
        setText(text);
        setContentDisplay(ContentDisplay.CENTER);
        setGraphicTextGap(20);
        setPrefSize(70, 28);
        setTextAlignment(TextAlignment.CENTER);
        setOnAction(handler);
        GridPane.setHalignment(this, HPos.RIGHT);
        GridPane.setValignment(this, VPos.BOTTOM);
    }

}
