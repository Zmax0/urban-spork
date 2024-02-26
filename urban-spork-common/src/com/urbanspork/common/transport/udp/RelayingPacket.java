package com.urbanspork.common.transport.udp;

import java.net.InetSocketAddress;

public record RelayingPacket<T>(InetSocketAddress address, T content) {}
