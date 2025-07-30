package com.urbanspork.test;

import com.urbanspork.common.config.DnsSetting;

import java.util.Optional;

public class DnsUtil {
    public static DnsSetting getDnsSetting() {
        String nameServer = Optional.ofNullable(System.getProperty("com.urbanspork.test.dns.name.server")).orElse("https://8.8.8.8/dns-query");
        return new DnsSetting(nameServer, null, 256);
    }
}
