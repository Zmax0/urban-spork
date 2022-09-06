package com.urbanspork.client.gui.console.unit;

import com.urbanspork.common.cipher.ShadowsocksCiphers;
import javafx.scene.control.ChoiceBox;
import javafx.scene.effect.BlendMode;

public class CurrentConfigCipherChoiceBox extends ChoiceBox<ShadowsocksCiphers> {

    public CurrentConfigCipherChoiceBox() {
        setBlendMode(BlendMode.HARD_LIGHT);
    }

}
