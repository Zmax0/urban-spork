package com.urbanspork.client.mvc;

import com.urbanspork.client.mvc.component.Console;
import com.urbanspork.client.mvc.component.Tray;

public class Main {

    public static void main(String[] args) throws Exception {
        Tray.launch(args);
        Console.launch(args);
    }

}
