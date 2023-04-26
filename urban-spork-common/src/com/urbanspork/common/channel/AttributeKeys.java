package com.urbanspork.common.channel;

import com.urbanspork.common.config.ServerConfig;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

public class AttributeKeys {

    public static final AttributeKey<ServerConfig> SERVER_CONFIG = AttributeKey.newInstance("SERVER_CONFIG");
    public static final AttributeKey<InetSocketAddress> REPLAY_ADDRESS = AttributeKey.newInstance("REPLAY_ADDRESS");
    public static final AttributeKey<Integer> SOCKS_PORT = AttributeKey.newInstance("SOCKS5_PORT");
    public static final AttributeKey<InetSocketAddress> SOCKS5_DST_ADDR = AttributeKey.newInstance("SOCKS5_DST_ADDR");

    private AttributeKeys() {

    }
}
