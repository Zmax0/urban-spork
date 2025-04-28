package com.urbanspork.common.config;

import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Test;

class SslSettingTest {
    @Test
    void testGetterAndSetter() {
        SslSetting setting = new SslSetting();
        TestUtil.testGetterAndSetter("A", setting, SslSetting::getCertificateFile, SslSetting::setCertificateFile);
        TestUtil.testGetterAndSetter("B", setting, SslSetting::getKeyFile, SslSetting::setKeyFile);
        TestUtil.testGetterAndSetter("C", setting, SslSetting::getKeyPassword, SslSetting::setKeyPassword);
        TestUtil.testGetterAndSetter("D", setting, SslSetting::getServerName, SslSetting::setServerName);
    }
}
