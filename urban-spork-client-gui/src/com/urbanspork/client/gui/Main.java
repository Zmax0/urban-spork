package com.urbanspork.client.gui;

import com.urbanspork.client.gui.console.component.Console;
import com.urbanspork.client.gui.console.component.Tray;

public final class Main {

    public static void main(String[] args) throws Exception {
        Tray.launch(args);
        Console.launch(args);
    }

}
