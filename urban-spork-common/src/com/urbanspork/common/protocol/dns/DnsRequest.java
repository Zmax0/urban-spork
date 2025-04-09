package com.urbanspork.common.protocol.dns;

import com.urbanspork.common.config.SslSetting;

import java.net.InetSocketAddress;

public record DnsRequest<T>(InetSocketAddress address, SslSetting ssl, T msg) {}
