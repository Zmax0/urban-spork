package com.urbanspork.common.transport.udp;

import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

public record TernaryDatagramPacket(DatagramPacket packet, InetSocketAddress third) {
    @Override
    public String toString() {
        if (packet.sender() != null) {
            return String.format("%s → %s ~ %s", packet.sender(), packet.recipient(), third);
        } else {
            return String.format("→ %s ~ %s", packet.recipient(), third);
        }
    }
}
