package com.urbanspork.client.gui;

import com.urbanspork.common.config.ClientConfig;
import com.urbanspork.common.config.ConfigHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Resource {

    private static final Logger logger = LoggerFactory.getLogger(Resource.class);

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
        String resourcePath = "resource/";
        PROGRAM_ICON = Objects.requireNonNull(classLoader.getResource(resourcePath + PROGRAM_ICON_NAME));
        TRAY_ICON = Objects.requireNonNull(classLoader.getResource(resourcePath + TRAY_ICON_NAME));
        CONSOLE_CSS = Objects.requireNonNull(classLoader.getResource(resourcePath + CONSOLE_CSS_NAME));
        ClientConfig config = null;
        try {
            config = ConfigHandler.read(ClientConfig.class);
        } catch (IOException e) {
            logger.error("Failed to load config file", e);
        }
        if (config == null) {
            config = new ClientConfig();
            config.setServers(new ArrayList<>(16));
        }
        String language = config.getLanguage();
        ResourceBundle bundle;
        String baseName = "com.urbanspork.client.gui.i18n.language";
        try {
            if (language == null) {
                Locale locale = Locale.getDefault();
                bundle = ResourceBundle.getBundle(baseName, locale);
                config.setLanguage(locale.getLanguage());
            } else {
                bundle = ResourceBundle.getBundle(baseName, new Locale(language));
            }
        } catch (MissingResourceException e) {
            bundle = ResourceBundle.getBundle(baseName, Locale.ENGLISH);
        }
        CONFIG = config;
        BUNDLE = bundle;
    }

    private Resource() {}

    public static ClientConfig config() {
        return CONFIG;
    }

    public static ResourceBundle bundle() {
        return BUNDLE;
    }

}
