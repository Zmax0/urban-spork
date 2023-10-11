package com.urbanspork.common.protocol.shadowsocks;

import com.urbanspork.common.protocol.network.Network;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public record RequestHeader(Network network, StreamType streamType, Socks5CommandRequest request) {}
