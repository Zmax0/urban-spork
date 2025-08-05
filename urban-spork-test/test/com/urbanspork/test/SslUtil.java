package com.urbanspork.test;

import com.urbanspork.common.config.SslSetting;

import java.util.Objects;

public class SslUtil {
    private SslUtil() {}

    public static SslSetting getSslSetting() {
        ClassLoader classLoader = SslUtil.class.getClassLoader();
        SslSetting sslSetting = new SslSetting();
        sslSetting.setCertificateFile(Objects.requireNonNull(classLoader.getResource("localhost.crt")).getFile());
        sslSetting.setKeyFile(Objects.requireNonNull(classLoader.getResource("localhost.key")).getFile());
        sslSetting.setServerName("localhost");
        return sslSetting;
    }
}
