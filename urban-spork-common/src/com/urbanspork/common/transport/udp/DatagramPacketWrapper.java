package com.urbanspork.common.transport.udp;

import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCounted;

import java.net.InetSocketAddress;

public record DatagramPacketWrapper(DatagramPacket packet, InetSocketAddress proxy) implements ReferenceCounted {
    @Override
    public String toString() {
        if (packet.sender() != null) {
            return String.format("%s → %s ~ %s", packet.sender(), packet.recipient(), proxy);
        } else {
            return String.format("→ %s ~ %s", packet.recipient(), proxy);
        }
    }

    @Override
    public int refCnt() {
        return packet.refCnt();
    }

    @Override
    public ReferenceCounted retain() {
        packet.retain();
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        packet.retain(increment);
        return this;
    }

    @Override
    public ReferenceCounted touch() {
        packet.touch();
        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        packet.touch(hint);
        return this;
    }

    @Override
    public boolean release() {
        return packet.release();
    }

    @Override
    public boolean release(int decrement) {
        return packet.release(decrement);
    }
}
