package com.urbanspork.common.protocol.shadowsocks;

import com.urbanspork.common.protocol.network.Network;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public record Context(Network network, StreamType streamType, Socks5CommandRequest request, Session session) {
    public Context(Network network, StreamType streamType, Socks5CommandRequest request) {
        this(network, streamType, request, new Session(1, 0, 0));
    }
}
