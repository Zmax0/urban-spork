package com.urbanspork.client.mvc;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.urbanspork.config.ClientConfig;
import com.urbanspork.config.ConfigHandler;

public class Resource {

    private static final String resource = "resource";
    private static final String trayIconName = "icon.png";
    private static final String clientFxmlName = "client.fxml";

    public static final URL TRAY_ICON;
    public static final URL CLIENT_FXML;
    public static final ClientConfig CLIENT_CONFIG;

    public static ResourceBundle bundle;

    static {
        try {
            CLIENT_CONFIG = ConfigHandler.read(ClientConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClassLoader classLoader = Resource.class.getClassLoader();
        TRAY_ICON = classLoader.getResource(resource + "/" + trayIconName);
        CLIENT_FXML = classLoader.getResource(resource + "/" + clientFxmlName);
        String language = CLIENT_CONFIG.getLanguage();
        try {
            if (language == null) {
                Locale locale = Locale.getDefault();
                bundle = ResourceBundle.getBundle("com.urbanspork.client.mvc.i18n.language", locale);
                CLIENT_CONFIG.setLanguage(locale.getLanguage());
            } else {
                bundle = ResourceBundle.getBundle("com.urbanspork.client.mvc.i18n.language", new Locale(language));
            }
        } catch (MissingResourceException e) {
            bundle = ResourceBundle.getBundle("com.urbanspork.client.mvc.i18n.language", new Locale("en"));
        }
    }

}
