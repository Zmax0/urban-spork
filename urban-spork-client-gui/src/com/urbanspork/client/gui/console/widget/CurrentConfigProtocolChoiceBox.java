package com.urbanspork.client.gui.console.widget;

import com.urbanspork.common.protocol.Protocols;
import javafx.scene.control.ChoiceBox;
import javafx.scene.effect.BlendMode;

public class CurrentConfigProtocolChoiceBox extends ChoiceBox<Protocols> {

    public CurrentConfigProtocolChoiceBox() {
        setBlendMode(BlendMode.HARD_LIGHT);
    }

}
