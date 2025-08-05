package com.urbanspork.common.codec.address;

import java.net.InetSocketAddress;

public record MaybeResolved(InetSocketAddress original, InetSocketAddress resolved, InetSocketAddress address) {
    public MaybeResolved(InetSocketAddress original, InetSocketAddress resolved) {
        this(original, resolved, resolved);
    }

    public MaybeResolved(InetSocketAddress peer) {
        this(peer, null, peer);
    }
}