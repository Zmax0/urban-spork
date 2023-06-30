package com.urbanspork.common.channel;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.network.Direction;
import com.urbanspork.common.protocol.network.Network;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.Map;

public class AttributeKeys {

    public static final AttributeKey<ServerConfig> SERVER_CONFIG = AttributeKey.newInstance("SERVER_CONFIG");
    public static final AttributeKey<Integer> SOCKS_PORT = AttributeKey.newInstance("SOCKS5_PORT");
    public static final AttributeKey<Map<InetSocketAddress, InetSocketAddress>> CALLBACK = AttributeKey.newInstance("UDP_CALLBACK");
    public static final AttributeKey<Network> NETWORK = AttributeKey.newInstance("NETWORK");
    public static final AttributeKey<Direction> DIRECTION = AttributeKey.newInstance("DIRECTION");

    private AttributeKeys() {}
}
