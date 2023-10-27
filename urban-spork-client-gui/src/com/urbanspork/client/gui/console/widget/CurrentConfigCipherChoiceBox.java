package com.urbanspork.client.gui.console.widget;

import com.urbanspork.common.codec.CipherKind;
import javafx.scene.control.ChoiceBox;
import javafx.scene.effect.BlendMode;

public class CurrentConfigCipherChoiceBox extends ChoiceBox<CipherKind> {

    public CurrentConfigCipherChoiceBox() {
        setBlendMode(BlendMode.HARD_LIGHT);
    }

}
