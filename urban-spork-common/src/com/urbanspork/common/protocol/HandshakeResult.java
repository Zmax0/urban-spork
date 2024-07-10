package com.urbanspork.common.protocol;

import io.netty.channel.Channel;

public record HandshakeResult<T>(Channel channel, T response) {}
