package com.urbanspork.client.trojan;

import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.protocol.trojan.Trojan;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;

class ClientUdpOverTcpTest {
    @ParameterizedTest
    @ValueSource(ints = {4096, 40000})
    void testDecodeFragmented(int size) {
        InetSocketAddress target = new InetSocketAddress("127.0.0.1", 12345);
        InetSocketAddress sender = new InetSocketAddress("127.0.0.1", 54321);
        byte[][] packets = {Dice.rollBytes(size), Dice.rollBytes(64)};
        ByteBuf wire = Unpooled.buffer();
        EmbeddedChannel downstream = new EmbeddedChannel();
        EmbeddedChannel channel = new EmbeddedChannel(new ClientUdpOverTcp.InboundHandler(downstream, sender));
        try {
            for (byte[] packet : packets) {
                Address.encode(target, wire);
                wire.writeShort(packet.length).writeBytes(Trojan.CRLF).writeBytes(packet);
            }
            channel.writeInbound(wire.readRetainedSlice(1));
            Assertions.assertNull(downstream.readOutbound());
            channel.writeInbound(wire.readRetainedSlice(4));
            Assertions.assertNull(downstream.readOutbound());
            channel.writeInbound(wire.readRetainedSlice(2043));
            Assertions.assertNull(downstream.readOutbound());
            channel.writeInbound(wire.readRetainedSlice(wire.readableBytes()));
            for (byte[] expected : packets) {
                DatagramPacketWrapper actual = downstream.readOutbound();
                Assertions.assertNotNull(actual);
                try {
                    Assertions.assertEquals(target, actual.packet().recipient());
                    Assertions.assertEquals(sender, actual.server());
                    Assertions.assertArrayEquals(expected, ByteBufUtil.getBytes(actual.packet().content()));
                } finally {
                    actual.release();
                }
            }
            Assertions.assertNull(downstream.readOutbound());
        } finally {
            wire.release();
            channel.finishAndReleaseAll();
            downstream.finishAndReleaseAll();
        }
    }
}
