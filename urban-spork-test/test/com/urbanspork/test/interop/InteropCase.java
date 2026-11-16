package com.urbanspork.test.interop;

import com.urbanspork.common.codec.CipherKind;

record InteropCase(Role role, CipherKind cipher, Carrier carrier, Traffic traffic) {
    enum Role { CLIENT, SERVER }
    enum Carrier { TCP, UDP, TLS, WS, WSS }
    enum Traffic { TCP, UDP }

    boolean tls() {
        return carrier == Carrier.TLS || carrier == Carrier.WSS;
    }

    boolean websocket() {
        return carrier == Carrier.WS || carrier == Carrier.WSS;
    }

    @Override
    public String toString() {
        return (role + "-" + (cipher == null ? "password" : cipher) + "-" + carrier + "-" + traffic).toLowerCase(java.util.Locale.ROOT);
    }
}
