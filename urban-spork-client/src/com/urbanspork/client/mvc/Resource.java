package com.urbanspork.client.mvc;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Resource {

    private static final Logger logger = LoggerFactory.getLogger(Resource.class);

    private static final String JAR = "jar";
    private static final String SEPARATOR = "/";
    private static final String RESOURCE = "resource";
    private static final String TRAY_ICON_NAME = "icon.png";
    private static final String CLIENT_FXML_NAME = "client.fxml";
    private static final String CONFIG_JSON_NAME = "config.json";
    private static final char separatorChar = File.separatorChar;

    public static final File TRAY_ICON;
    public static final File CLIENT_FXML;
    public static final File CONFIG_JSON;

    static {
        String path = Resource.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        if (path.endsWith(JAR)) {
            path = path.substring(0, path.lastIndexOf(SEPARATOR));
        }
        logger.info("Resource path: " + path);
        TRAY_ICON = new File(path + separatorChar + RESOURCE + separatorChar + TRAY_ICON_NAME);
        CLIENT_FXML = new File(path + separatorChar + RESOURCE + separatorChar + CLIENT_FXML_NAME);
        CONFIG_JSON = new File(path + separatorChar + CONFIG_JSON_NAME);
    }

}
