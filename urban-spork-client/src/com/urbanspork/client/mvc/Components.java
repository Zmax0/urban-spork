package com.urbanspork.client.mvc;

import com.urbanspork.client.mvc.component.Console;
import com.urbanspork.client.mvc.component.Controller;
import com.urbanspork.client.mvc.component.Tray;

public class Components {

    public static Controller Controller;
    public static Console Console;
    public static Tray Tray;

    public static final void register(Object object) {
        if (object != null) {
            if (object instanceof Controller) {
                Controller = (Controller) object;
            } else if (object instanceof Console) {
                Console = (Console) object;
            } else if (object instanceof Tray) {
                Tray = (Tray) object;
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            throw new NullPointerException();
        }
    }

}