package com.urbanspork.common.config;

import java.io.File;

public class ConfigLocation {

    private static final String LIB = "lib";

    private ConfigLocation() {}

    public static String getPath(Class<?> clazz) {
        File file = new File(clazz.getProtectionDomain().getCodeSource().getLocation().getFile());
        File parentFile = file.getParentFile();
        if (parentFile.getName().endsWith(LIB)) {
            parentFile = parentFile.getParentFile();
        }
        return parentFile.getAbsolutePath();
    }

}
