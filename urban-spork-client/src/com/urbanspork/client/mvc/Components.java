package com.urbanspork.client.mvc;

import com.urbanspork.client.mvc.component.Console;
import com.urbanspork.client.mvc.component.Tray;

public class Components {

    public static Tray TRAY;
    public static Console CONSOLE;

    public static final void register(Object object) {
        if (object != null) {
            if (object instanceof Console) {
                CONSOLE = (Console) object;
            } else if (object instanceof Tray) {
                TRAY = (Tray) object;
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            throw new NullPointerException();
        }
    }

}