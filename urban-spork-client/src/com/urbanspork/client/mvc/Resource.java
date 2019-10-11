package com.urbanspork.client.mvc;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.urbanspork.config.ClientConfig;
import com.urbanspork.config.ConfigHandler;

public class Resource {

    private static final String RESOURCE = "resource";
    private static final String TRAY_ICON_NAME = "ticon.png";
    private static final String PROGRAM_ICON_NAME = "picon.png";
    private static final String CONSOLE_CSS_NAME = "console.css";
    private static final ClientConfig CONFIG;
    private static final ResourceBundle BUNDLE;

    public static final URL PROGRAM_ICON;
    public static final URL TRAY_ICON;
    public static final URL CONSOLE_CSS;

    static {
        ClassLoader classLoader = Resource.class.getClassLoader();
        PROGRAM_ICON = classLoader.getResource(RESOURCE + "/" + PROGRAM_ICON_NAME);
        TRAY_ICON = classLoader.getResource(RESOURCE + "/" + TRAY_ICON_NAME);
        CONSOLE_CSS = classLoader.getResource(RESOURCE + "/" + CONSOLE_CSS_NAME);
        ClientConfig config = null;
        try {
            config = ConfigHandler.read(ClientConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file", e);
        }
        if (config == null) {
            config = new ClientConfig();
            config.setServers(new ArrayList<>(16));
        }
        String language = config.getLanguage();
        ResourceBundle bundle = null;
        try {
            if (language == null) {
                Locale locale = Locale.getDefault();
                bundle = ResourceBundle.getBundle("com.urbanspork.client.mvc.i18n.language", locale);
                config.setLanguage(locale.getLanguage());
            } else {
                bundle = ResourceBundle.getBundle("com.urbanspork.client.mvc.i18n.language", new Locale(language));
            }
        } catch (MissingResourceException e) {
            bundle = ResourceBundle.getBundle("com.urbanspork.client.mvc.i18n.language", Locale.ENGLISH);
        }
        CONFIG = config;
        BUNDLE = bundle;
    }

    public static ClientConfig config() {
        return CONFIG;
    }

    public static ResourceBundle bundle() {
        return BUNDLE;
    }

}
