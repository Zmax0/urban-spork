package com.urbanspork.common.transport.udp;

import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

public record DatagramPacketWrapper(DatagramPacket packet, InetSocketAddress proxy) {
    @Override
    public String toString() {
        if (packet.sender() != null) {
            return String.format("%s → %s ~ %s", packet.sender(), packet.recipient(), proxy);
        } else {
            return String.format("→ %s ~ %s", packet.recipient(), proxy);
        }
    }
}
