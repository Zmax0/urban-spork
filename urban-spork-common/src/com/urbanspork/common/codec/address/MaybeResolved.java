package com.urbanspork.common.codec.address;

import java.net.InetSocketAddress;

public record MaybeResolved(InetSocketAddress original, InetSocketAddress resolved) {
    public MaybeResolved(InetSocketAddress peer) {
        this(peer, null);
    }

    public InetSocketAddress address() {
        return resolved == null ? original : resolved;
    }
}