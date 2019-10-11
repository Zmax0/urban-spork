package com.urbanspork.client.mvc.component.element;

import com.urbanspork.cipher.ShadowsocksCiphers;

import javafx.scene.control.ChoiceBox;
import javafx.scene.effect.BlendMode;

public class CurrentConfigCipherChoiceBox extends ChoiceBox<ShadowsocksCiphers> {

    public CurrentConfigCipherChoiceBox() {
        setBlendMode(BlendMode.HARD_LIGHT);
    }

}
