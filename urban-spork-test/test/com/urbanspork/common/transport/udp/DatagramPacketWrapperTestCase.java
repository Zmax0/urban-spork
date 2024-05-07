package com.urbanspork.common.transport.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

@DisplayName("Common - Datagram Packet Wrapper")
class DatagramPacketWrapperTestCase {
    @Test
    void test() {
        ByteBuf buffer = Unpooled.buffer();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 1234);
        DatagramPacket packet = new DatagramPacket(buffer, address);
        DatagramPacketWrapper wrapper = new DatagramPacketWrapper(packet, address);
        wrapper.retain(2);
        Assertions.assertEquals(buffer.refCnt(), wrapper.refCnt());
        wrapper.touch();
        wrapper.release(2);
        Assertions.assertEquals(buffer.refCnt(), wrapper.refCnt());
    }
}