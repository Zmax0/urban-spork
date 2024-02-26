package com.urbanspork.common.transport.tcp;

import java.net.InetSocketAddress;

public record RelayingPayload<T>(InetSocketAddress address, T content) {}
