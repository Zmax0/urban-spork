package com.urbanspork.common.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.urbanspork.common.protocol.dns.Cache;

import java.util.Optional;

public record DnsSetting(String nameServer, SslSetting ssl, Cache cache) {
    private static final int DEFAULT_CACHE_SIZE = 256;

    @JsonCreator
    public DnsSetting(@JsonProperty(value = "nameServer", required = true) String nameServer, @JsonProperty(value = "ssl") SslSetting ssl, @JsonProperty(value = "cacheSize") Integer cacheSize) {
        this(nameServer, ssl, new Cache(Optional.ofNullable(cacheSize).orElse(DEFAULT_CACHE_SIZE)));
    }

    public DnsSetting(String nameServer, SslSetting ssl) {
        this(nameServer, ssl, new Cache(DEFAULT_CACHE_SIZE));
    }
}
