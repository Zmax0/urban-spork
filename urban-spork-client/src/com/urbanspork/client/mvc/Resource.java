package com.urbanspork.client.mvc;

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Resource {

    private static final String RESOURCE = "resource";
    private static final String TRAY_ICON_NAME = "icon.png";
    private static final String CLIENT_FXML_NAME = "client.fxml";

    public static final URL TRAY_ICON;
    public static final URL CLIENT_FXML;

    public static ResourceBundle bundle;

    static {
        ClassLoader classLoader = Resource.class.getClassLoader();
        TRAY_ICON = classLoader.getResource(RESOURCE + "/" + TRAY_ICON_NAME);
        CLIENT_FXML = classLoader.getResource(RESOURCE + "/" + CLIENT_FXML_NAME);
        try {
            bundle = ResourceBundle.getBundle("com.urbanspork.client.mvc.i18n.language", Locale.getDefault());
        } catch (MissingResourceException e) {
            bundle = ResourceBundle.getBundle("com.urbanspork.client.mvc.i18n.language", new Locale("en"));
        }
    }

}
