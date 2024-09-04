package com.urbanspork.client;

import com.urbanspork.common.config.ClientConfig;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;

public record ClientInitializationContext(ClientConfig config, GlobalChannelTrafficShapingHandler traffic) {}
