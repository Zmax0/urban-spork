package com.urbanspork.common.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

class FileConfigHolder implements ConfigHolder {

    private static final String FILE_NAME = "config.json";
    private static final String FOLDER_NAME = "lib";
    private static final Path PATH;

    static {
        String pathStr = System.getProperty("com.urbanspork.configPath");
        if (pathStr != null) {
            PATH = Path.of(pathStr);
        } else {
            PATH = Path.of(parentPath(), FILE_NAME);
        }
    }

    private static String parentPath() {
        File file = new File(FileConfigHolder.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        File parentFile = file.getParentFile();
        if (parentFile.getName().endsWith(FOLDER_NAME)) {
            parentFile = parentFile.getParentFile();
        }
        return parentFile.getAbsolutePath();
    }

    FileConfigHolder() {}

    @Override
    public void save(String str) throws IOException {
        if (!Files.exists(PATH)) {
            Files.createFile(PATH);
        }
        try (
            FileWriter writer = new FileWriter(PATH.toFile())) {
            writer.write(str);
            writer.flush();
        }
    }

    @Override
    public String read() throws IOException {
        if (Files.exists(PATH)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(PATH.toFile()))) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } else {
            throw new IllegalArgumentException("File in path " + PATH + " is not exist");
        }
    }

    @Override
    public void delete() throws IOException {
        Files.deleteIfExists(PATH);
    }
}
