package com.urbanspork.config;

import java.io.File;

public class ConfigLocation {

    private static final String LIB = "lib";

    public static final String PATH;

    static {
        File file = new File(ConfigLocation.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        File parentFile = file.getParentFile();
        if (parentFile.getName().endsWith(LIB)) {
            parentFile = parentFile.getParentFile();
        }
        PATH = parentFile.getAbsolutePath();
    }

}
