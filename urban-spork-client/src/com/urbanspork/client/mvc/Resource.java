package com.urbanspork.client.mvc;

import java.io.File;

import com.urbanspork.config.ConfigLocation;

public class Resource {

    private static final String RESOURCE = "resource";
    private static final String TRAY_ICON_NAME = "icon.png";
    private static final String CLIENT_FXML_NAME = "client.fxml";

    public static final File TRAY_ICON;
    public static final File CLIENT_FXML;

    static {
        TRAY_ICON = new File(ConfigLocation.PATH + File.separatorChar + RESOURCE + File.separatorChar + TRAY_ICON_NAME);
        CLIENT_FXML = new File(ConfigLocation.PATH + File.separatorChar + RESOURCE + File.separatorChar + CLIENT_FXML_NAME);
    }

}
