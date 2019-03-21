package com.urbanspork.utils;

import java.io.File;
import java.io.FileInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;

public class ConfigUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    private static final String jar = "jar";
    private static final String separator = "/";
    private static final String name = "config.json";
    private static final JSONObject json;

    static {
        try {
            String path = ConfigUtils.class.getProtectionDomain().getCodeSource().getLocation().getFile();
            if (path.endsWith(jar)) {
                path = path.substring(0, path.lastIndexOf(separator));
            }
            logger.info("config.path:" + path);
            File file = new File(path + File.separatorChar + name);
            logger.info("config.exists:" + file.exists());
            json = JSON.parseObject(new FileInputStream(file), JSONObject.class, Feature.IgnoreNotMatch);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T get(String key, Class<T> type) {
        if (json.containsKey(key)) {
            return json.getObject(key, type);
        } else {
            return null;
        }
    }
}
