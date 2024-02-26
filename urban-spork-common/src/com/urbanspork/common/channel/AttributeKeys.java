package com.urbanspork.common.channel;

import com.urbanspork.common.config.ServerConfig;
import io.netty.util.AttributeKey;

public class AttributeKeys {

    public static final AttributeKey<ServerConfig> SERVER_CONFIG = AttributeKey.newInstance("SERVER_CONFIG");
    public static final AttributeKey<Integer> SOCKS_PORT = AttributeKey.newInstance("SOCKS5_PORT");

    private AttributeKeys() {}
}
