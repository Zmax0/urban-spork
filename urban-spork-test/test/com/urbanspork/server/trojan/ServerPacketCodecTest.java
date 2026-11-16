package com.urbanspork.server.trojan;

import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.protocol.trojan.Trojan;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;

class ServerPacketCodecTest {
    @ParameterizedTest
    @ValueSource(ints = {4096, 40000})
    void decodesFragmentedAndCoalescedPackets(int size) {
        InetSocketAddress target = new InetSocketAddress("127.0.0.1", 12345);
        InetSocketAddress sender = new InetSocketAddress("127.0.0.1", 54321);
        byte[][] packets = {Dice.rollBytes(size), Dice.rollBytes(64)};
        ByteBuf wire = Unpooled.buffer();
        EmbeddedChannel channel = new EmbeddedChannel(new ServerPacketCodec(sender));
        try {
            for (byte[] packet : packets) {
                Address.encode(target, wire);
                wire.writeShort(packet.length).writeBytes(Trojan.CRLF).writeBytes(packet);
            }
            Assertions.assertFalse(channel.writeInbound(wire.readRetainedSlice(1)));
            Assertions.assertFalse(channel.writeInbound(wire.readRetainedSlice(4)));
            Assertions.assertFalse(channel.writeInbound(wire.readRetainedSlice(2043)));
            Assertions.assertTrue(channel.writeInbound(wire.readRetainedSlice(wire.readableBytes())));
            for (byte[] expected : packets) {
                DatagramPacket actual = channel.readInbound();
                Assertions.assertNotNull(actual);
                try {
                    Assertions.assertEquals(target, actual.recipient());
                    Assertions.assertEquals(sender, actual.sender());
                    Assertions.assertArrayEquals(expected, ByteBufUtil.getBytes(actual.content()));
                } finally {
                    actual.release();
                }
            }
            Assertions.assertNull(channel.readInbound());
        } finally {
            wire.release();
            channel.finishAndReleaseAll();
        }
    }
}
