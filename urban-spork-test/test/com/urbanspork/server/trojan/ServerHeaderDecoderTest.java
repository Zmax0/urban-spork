package com.urbanspork.server.trojan;

import com.urbanspork.common.config.ServerConfigTest;
import com.urbanspork.common.crypto.Digests;
import com.urbanspork.common.protocol.socks.Address;
import com.urbanspork.common.protocol.trojan.Trojan;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socks.SocksCmdType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

class ServerHeaderDecoderTest {
    @Test
    void testInvalidServerHeader() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new ServerHeaderDecoder(ServerConfigTest.testConfig(0)));
        ByteBuf msg = Unpooled.buffer();
        msg.writeByte(0);
        String key = ByteBufUtil.hexDump(Dice.rollBytes(Digests.sha224.size()));
        msg.writeCharSequence(key, StandardCharsets.US_ASCII);
        channel.writeInbound(msg.retain());
        msg.writeShort(0);
        msg.writeByte(SocksCmdType.CONNECT.byteValue());
        channel.writeInbound(msg.retain());
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
        Address.encode(address, msg);
        msg.retain();
        Assertions.assertThrows(DecoderException.class, () -> channel.writeInbound(msg));
        msg.clear();
        msg.writeCharSequence(key, StandardCharsets.US_ASCII);
        msg.writeBytes(Trojan.CRLF);
        msg.writeByte(SocksCmdType.CONNECT.byteValue());
        Address.encode(address, msg);
        Assertions.assertThrows(DecoderException.class, () -> channel.writeInbound(msg));
    }
}
