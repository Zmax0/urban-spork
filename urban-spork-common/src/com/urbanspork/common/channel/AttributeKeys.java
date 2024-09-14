package com.urbanspork.common.channel;

import com.urbanspork.common.config.ServerConfig;
import io.netty.util.AttributeKey;

public class AttributeKeys {

    public static final AttributeKey<ServerConfig> SERVER_CONFIG = AttributeKey.newInstance("SERVER_CONFIG");
    public static final AttributeKey<Object> SERVER_UDP_RELAY_WORKER = AttributeKey.newInstance("SERVER_UDP_RELAY_WORKER");

    private AttributeKeys() {}
}
