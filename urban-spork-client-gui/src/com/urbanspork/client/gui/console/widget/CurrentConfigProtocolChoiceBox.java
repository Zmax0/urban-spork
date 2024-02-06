package com.urbanspork.client.gui.console.widget;

import com.urbanspork.common.protocol.Protocol;
import javafx.scene.control.ChoiceBox;
import javafx.scene.effect.BlendMode;

public class CurrentConfigProtocolChoiceBox extends ChoiceBox<Protocol> {

    public CurrentConfigProtocolChoiceBox() {
        setBlendMode(BlendMode.HARD_LIGHT);
    }

}
