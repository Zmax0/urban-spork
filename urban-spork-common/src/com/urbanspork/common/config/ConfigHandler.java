package com.urbanspork.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.CharsetUtil;

import java.io.*;

public class ConfigHandler {

    private static final String NAME = "config.json";
    private static final File FILE = new File(ConfigLocation.getPath(ConfigHandler.class) + File.separatorChar + NAME);

    private ConfigHandler() {

    }

    public static void write(Object object) throws IOException {
        if (!FILE.exists() && !FILE.getParentFile().mkdirs() && !FILE.createNewFile()) {
            throw new IllegalStateException("failed to create config file");
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(FILE), CharsetUtil.UTF_8.name())) {
            ObjectMapper mapper = new ObjectMapper();
            writer.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object));
            writer.flush();
        }
    }

    public static <T> T read(Class<T> clazz) throws IOException {
        T t = null;
        if (FILE.exists()) {
            StringBuilder builder = new StringBuilder();
            char[] cbuf = new char[1];
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(FILE), CharsetUtil.UTF_8.name())) {
                while (reader.read(cbuf) != -1) {
                    builder.append(cbuf);
                }
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            t = mapper.readValue(builder.toString(), clazz);
        }
        return t;
    }
}
