package com.urbanspork.client;

import com.urbanspork.common.config.ServerConfig;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;

import java.util.Map;

public record ClientChannelContext(ServerConfig config, GlobalChannelTrafficShapingHandler traffic, Map<String, ClientChannelTrafficHandler> channelTraffic) {

}
