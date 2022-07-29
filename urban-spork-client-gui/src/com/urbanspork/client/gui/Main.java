package com.urbanspork.client.gui;

import com.urbanspork.client.gui.console.component.Console;
import javafx.application.Application;

public final class Main {

    public static void main(String[] args) {
        System.setProperty("javafx.preloader", "com.urbanspork.client.gui.console.component.Console");
        Application.launch(Console.class, args);
    }

}
