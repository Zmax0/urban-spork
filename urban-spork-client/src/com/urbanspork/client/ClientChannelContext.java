package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import io.netty.util.AttributeKey;

import java.util.Map;

public record ClientChannelContext(ServerConfig config, GlobalChannelTrafficShapingHandler traffic, Map<String, ClientChannelTrafficHandler> channelTraffic) {
    public static final AttributeKey<ClientChannelContext> KEY = AttributeKey.newInstance("CLIENT_CHANNEL_CONTEXT");
}
