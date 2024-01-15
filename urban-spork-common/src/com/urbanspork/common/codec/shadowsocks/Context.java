package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.network.Network;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public record Context(Network network, Mode mode, Session session, Control control, Socks5CommandRequest request, ServerUserManager userManager) {
    Context(Network network, Mode mode, CipherKind kind, Socks5CommandRequest request, ServerUserManager userManager) {
        this(network, mode, getSession(network, kind), new Control(), request, userManager);
    }

    private static Session getSession(Network network, CipherKind kind) {
        if (Network.UDP == network) {
            return Session.udp(kind);
        } else {
            return Session.tcp(kind);
        }
    }
}
