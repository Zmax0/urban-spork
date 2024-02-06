package com.urbanspork.common.protocol.socks;

import com.urbanspork.common.codec.socks.DatagramPacketDecoder;
import com.urbanspork.common.codec.socks.DatagramPacketEncoder;
import com.urbanspork.common.transport.udp.TernaryDatagramPacket;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@DisplayName("Socks - Datagram Packet")
class DatagramPacketTestCase {
    @Test
    void testCodec() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(
            new DatagramPacketDecoder(),
            new DatagramPacketEncoder()
        );
        String str = TestDice.rollString();
        int dstPort = TestDice.rollPort();
        int socksPort = TestDice.rollPort();
        InetSocketAddress dstAddress = new InetSocketAddress(dstPort);
        InetSocketAddress socksAddress = new InetSocketAddress(socksPort);
        channel.writeOutbound(new TernaryDatagramPacket(new DatagramPacket(Unpooled.wrappedBuffer(str.getBytes()), dstAddress), socksAddress));
        DatagramPacket outbound = channel.readOutbound();
        Assertions.assertEquals(socksAddress, outbound.recipient());
        DatagramPacket outbound2 = outbound.replace(outbound.content().copy(0, 4));
        Assertions.assertThrows(DecoderException.class, () -> channel.writeInbound(outbound2));
        DatagramPacket outbound3 = outbound.replace(outbound.content().copy().setByte(2, 1));
        Assertions.assertThrows(DecoderException.class, () -> channel.writeInbound(outbound3));
        channel.writeInbound(outbound);
        TernaryDatagramPacket inbound = channel.readInbound();
        Assertions.assertEquals(dstAddress, inbound.third());
        Assertions.assertEquals(socksAddress, inbound.packet().recipient());
        ByteBuf content = inbound.packet().content();
        Assertions.assertEquals(str, content.readCharSequence(content.readableBytes(), StandardCharsets.UTF_8));
    }
}
