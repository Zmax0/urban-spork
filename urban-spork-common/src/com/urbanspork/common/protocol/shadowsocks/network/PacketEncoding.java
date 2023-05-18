package com.urbanspork.common.protocol.shadowsocks.network;

import java.net.InetSocketAddress;

public enum PacketEncoding {

    None(null),
    Packet(InetSocketAddress.createUnresolved("sp.packet-addr.v2fly.arpa", 0));

    private final InetSocketAddress address;

    PacketEncoding(InetSocketAddress address) {
        this.address = address;
    }

    public InetSocketAddress seqPacketMagicAddress() {
        return address;
    }
}
