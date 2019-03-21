package com.urbanspork.client.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class ConfigHandler {

    private static final String CONFIG_NAME = "config.json";

    private File config;

    private boolean isNewFile;

    public ConfigHandler() {
        this(deaultConfig());
    }

    public ConfigHandler(File config) {
        this.config = config;
        if (!config.exists()) {
            try {
                config.createNewFile();
                isNewFile = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void write(Object object) {
        try (FileWriter writer = new FileWriter(config)) {
            writer.write(JSON.toJSONString(object, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(Class<T> clazz) {
        T t = null;
        if (!isNewFile) {
            StringBuilder builder = new StringBuilder();
            char[] cbuf = new char[1];
            try (FileReader reader = new FileReader(config)) {
                while (reader.read(cbuf) != -1) {
                    builder.append(cbuf);
                }
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            t = JSON.parseObject(builder.toString(), clazz);
        }
        return t;
    }

    private static File deaultConfig() {
        return new File(ConfigHandler.class.getProtectionDomain().getCodeSource().getLocation().getFile() + '/' + CONFIG_NAME);
    }
}
