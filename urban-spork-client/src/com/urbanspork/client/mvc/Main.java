package com.urbanspork.client.mvc;

import com.urbanspork.client.mvc.component.Console;
import com.urbanspork.client.mvc.component.Proxy;
import com.urbanspork.client.mvc.component.Tray;

public class Main {

    public static void main(String[] args) throws Exception {
        Tray tray = new Tray();
        tray.start(args);
        Console console = new Console();
        console.start(args);
        while (true) {
            if (tray.started() && console.started()) {
                Proxy.launch(args);
                break;
            }
        }
    }

}
