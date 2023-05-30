package com.urbanspork.common.protocol.socks;

import com.urbanspork.common.network.TernaryDatagramPacket;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

class Socks5DatagramPacketTestCase {

    @Test
    void testCodec() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(
                new Socks5DatagramPacketDecoder(),
                new Socks5DatagramPacketEncoder()
        );
        String str = TestDice.randomString();
        int dstPort = TestDice.randomPort();
        int socksPort = TestDice.randomPort();
        InetSocketAddress dstAddress = new InetSocketAddress(dstPort);
        InetSocketAddress socksAddress = new InetSocketAddress(socksPort);
        channel.writeOutbound(new TernaryDatagramPacket(new DatagramPacket(Unpooled.wrappedBuffer(str.getBytes()), dstAddress), socksAddress));
        DatagramPacket outbound = channel.readOutbound();
        Assertions.assertEquals(socksAddress, outbound.recipient());
        channel.writeInbound(outbound);
        TernaryDatagramPacket inbound = channel.readInbound();
        Assertions.assertEquals(dstAddress, inbound.third());
        Assertions.assertEquals(socksAddress, inbound.packet().recipient());
        ByteBuf content = inbound.packet().content();
        Assertions.assertEquals(str, content.readCharSequence(content.readableBytes(), StandardCharsets.UTF_8));
    }

}
