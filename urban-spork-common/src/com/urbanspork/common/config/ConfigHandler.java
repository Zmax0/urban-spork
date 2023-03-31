package com.urbanspork.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.stream.Collectors;

public class ConfigHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String NAME = "config.json";
    private static final File FILE = new File(ConfigLocation.getPath(ConfigHandler.class) + File.separatorChar + NAME);

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private ConfigHandler() {

    }

    public static void write(Object object) throws IOException {
        if (!FILE.exists() && !FILE.getParentFile().mkdirs() && !FILE.createNewFile()) {
            throw new IllegalStateException("failed to create config file");
        }
        try (FileWriter writer = new FileWriter(FILE)) {
            writer.write(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object));
            writer.flush();
        }
    }

    public static <T> T read(Class<T> clazz) throws IOException {
        if (FILE.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
                return MAPPER.readValue(reader.lines().collect(Collectors.joining()), clazz);
            }
        } else {
            return null;
        }
    }
}
