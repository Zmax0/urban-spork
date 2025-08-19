package com.urbanspork.common.protocol.dns;

import com.urbanspork.common.config.SslSetting;

import java.net.InetSocketAddress;

public record DnsRequest(InetSocketAddress address, SslSetting ssl, Object msg) {}
