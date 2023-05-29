package com.urbanspork.common.config;

import java.io.*;
import java.util.stream.Collectors;

class FileConfigHolder implements ConfigHolder {

    private static final String FILE_NAME = "config.json";
    private static final String FOLDER_NAME = "lib";
    private static final File FILE = new File(FileConfigHolder.parentPath() + File.separatorChar + FILE_NAME);

    FileConfigHolder() {}

    private static String parentPath() {
        File file = new File(FileConfigHolder.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        File parentFile = file.getParentFile();
        if (parentFile.getName().endsWith(FOLDER_NAME)) {
            parentFile = parentFile.getParentFile();
        }
        return parentFile.getAbsolutePath();
    }

    @Override
    public void write(String str) throws IOException {
        if (!FILE.exists() && !FILE.getParentFile().mkdirs() && !FILE.createNewFile()) {
            throw new IllegalStateException("failed to create config file");
        }
        try (FileWriter writer = new FileWriter(FILE)) {
            writer.write(str);
            writer.flush();
        }
    }

    @Override
    public String read() throws IOException {
        if (FILE.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } else {
            throw new IllegalArgumentException("Please put the '" + FILE_NAME + "' file into the folder");
        }
    }
}
