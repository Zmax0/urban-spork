package com.urbanspork.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import io.netty.util.CharsetUtil;

public class ConfigHandler {

    private static final String name = "config.json";
    public static final File config;

    static {
        try {
            config = new File(ConfigLocation.getPath(ConfigHandler.class) + File.separatorChar + name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(Object object) throws IOException {
        if (!config.exists()) {
            config.createNewFile();
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(config), CharsetUtil.UTF_8.name())) {
            writer.write(JSON.toJSONString(object, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat));
            writer.flush();
            writer.close();
        }
    }

    public static <T> T read(Class<T> clazz) throws IOException {
        T t = null;
        if (config.exists()) {
            StringBuilder builder = new StringBuilder();
            char[] cbuf = new char[1];
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(config), CharsetUtil.UTF_8.name())) {
                while (reader.read(cbuf) != -1) {
                    builder.append(cbuf);
                }
                reader.close();
            }
            t = JSON.parseObject(builder.toString(), clazz);
        }
        return t;
    }
}
