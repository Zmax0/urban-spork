package com.urbanspork.client.gui;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;

import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class Resource {

    private static final String TRAY_ICON_NAME = "ticon.png";
    private static final String PROGRAM_ICON_NAME = "picon.png";
    private static final String CONSOLE_CSS_NAME = "console.css";
    private static final ClientConfig CONFIG;

    public static final URL PROGRAM_ICON;
    public static final URL TRAY_ICON;
    public static final URL CONSOLE_CSS;

    static {
        String resourcePath = "/resource/";
        PROGRAM_ICON = Objects.requireNonNull(Resource.class.getResource(resourcePath + PROGRAM_ICON_NAME));
        TRAY_ICON = Objects.requireNonNull(Resource.class.getResource(resourcePath + TRAY_ICON_NAME));
        CONSOLE_CSS = Objects.requireNonNull(Resource.class.getResource(resourcePath + CONSOLE_CSS_NAME));
        ClientConfig config;
        try {
            config = ConfigHandler.DEFAULT.read();
        } catch (IllegalArgumentException _) {
            config = new ClientConfig();
            config.setServers(new ArrayList<>());
        }
        String language = config.getLanguage();
        if (language == null) {
            Locale locale = Locale.getDefault();
            config.setLanguage(locale.getLanguage());
        }
        CONFIG = config;
    }

    private Resource() {}

    public static ClientConfig config() {
        return CONFIG;
    }

    public static ResourceBundle language() {
        return ResourceBundle.getBundle("resource.locales.console", Locale.of(CONFIG.getLanguage()));
    }

    public static ResourceBundle application() {
        return ResourceBundle.getBundle("application");
    }
}
