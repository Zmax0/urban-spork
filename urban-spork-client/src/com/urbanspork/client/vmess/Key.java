package com.urbanspork.client.vmess;

import java.net.InetSocketAddress;

public record Key(InetSocketAddress sender, InetSocketAddress recipient) {
    @Override
    public String toString() {
        return "[" + sender + " - " + recipient + "]";
    }
}
