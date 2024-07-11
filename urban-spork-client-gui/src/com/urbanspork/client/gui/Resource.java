package com.urbanspork.client.gui;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;

import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

public class Resource {

    private static final String TRAY_ICON_NAME = "ticon.png";
    private static final String PROGRAM_ICON_NAME = "picon.png";
    private static final String CONSOLE_CSS_NAME = "console.css";
    private static final ClientConfig CONFIG;
    private static final ResourceBundle LANGUAGE;

    public static final URL PROGRAM_ICON;
    public static final URL TRAY_ICON;
    public static final URL CONSOLE_CSS;

    static {
        String resourcePath = "/resource/";
        PROGRAM_ICON = Objects.requireNonNull(Resource.class.getResource(resourcePath + PROGRAM_ICON_NAME));
        TRAY_ICON = Objects.requireNonNull(Resource.class.getResource(resourcePath + TRAY_ICON_NAME));
        CONSOLE_CSS = Objects.requireNonNull(Resource.class.getResource(resourcePath + CONSOLE_CSS_NAME));
        ClientConfig config = ConfigHandler.DEFAULT.read();
        if (config == null) {
            config = new ClientConfig();
            config.setServers(new ArrayList<>());
        }
        String language = config.getLanguage();
        ResourceBundle bundle;
        String baseName = "resource.locales.console";
        try {
            if (language == null) {
                Locale locale = Locale.getDefault();
                bundle = ResourceBundle.getBundle(baseName, locale);
                config.setLanguage(locale.getLanguage());
            } else {
                bundle = ResourceBundle.getBundle(baseName, Locale.of(language));
            }
        } catch (MissingResourceException e) {
            bundle = ResourceBundle.getBundle(baseName, Locale.ENGLISH);
        }
        CONFIG = config;
        LANGUAGE = bundle;
    }

    private Resource() {}

    public static ClientConfig config() {
        return CONFIG;
    }

    public static ResourceBundle language() {
        return LANGUAGE;
    }

    public static ResourceBundle application() {
        return ResourceBundle.getBundle("application");
    }
}
