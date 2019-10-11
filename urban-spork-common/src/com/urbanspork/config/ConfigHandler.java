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

    private static final String NAME = "config.json";
    private static final File FILE = new File(ConfigLocation.getPath(ConfigHandler.class) + File.separatorChar + NAME);;

    public static void write(Object object) throws IOException {
        if (!FILE.exists()) {
            FILE.createNewFile();
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(FILE), CharsetUtil.UTF_8.name())) {
            writer.write(JSON.toJSONString(object, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat));
            writer.flush();
            writer.close();
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
            t = JSON.parseObject(builder.toString(), clazz);
        }
        return t;
    }
}
