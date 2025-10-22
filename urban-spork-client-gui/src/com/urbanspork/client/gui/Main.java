package com.urbanspork.client.gui;

import com.urbanspork.client.gui.console.Console;
import javafx.application.Application;

public final class Main {
    static void main(String[] args) {
        System.setProperty("javafx.preloader", "com.urbanspork.client.gui.console.ConsolePreloader");
        Application.launch(Console.class, args);
    }
}
