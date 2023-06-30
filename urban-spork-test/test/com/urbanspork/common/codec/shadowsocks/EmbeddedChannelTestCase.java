package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.shadowsocks.AddressDecoder;
import com.urbanspork.common.protocol.shadowsocks.AddressEncoder;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@DisplayName("Shadowsocks - Embedded Channel")
class EmbeddedChannelTestCase {

    @ParameterizedTest
    @EnumSource(Network.class)
    void testCommonChannel(Network network) {
        int port = TestDice.rollPort();
        SupportedCipher cipher = TestDice.rollCipher();
        String password = TestDice.rollString();
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline()
            .addLast(AEADCipherCodecs.get(password, cipher, network))
            .addLast(new AddressEncoder(new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, "localhost", port)))
            .addLast(new AddressDecoder());
        String message = TestDice.rollString();
        channel.writeOutbound(Unpooled.wrappedBuffer(message.getBytes()));
        ByteBuf out = channel.readOutbound();
        channel.writeInbound(out);
        InetSocketAddress address = channel.readInbound();
        Assertions.assertEquals(address.getPort(), port);
        ByteBuf msg = channel.readInbound();
        Assertions.assertEquals(message, msg.readCharSequence(msg.readableBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void testUDPReplayChannel() {
        EmbeddedChannel channel = new EmbeddedChannel();
        SupportedCipher cipher = TestDice.rollCipher();
        int port = TestDice.rollPort();
        InetSocketAddress replay = new InetSocketAddress("192.168.1.1", port);
        ServerConfig config = new ServerConfig();
        config.setPassword(TestDice.rollString());
        config.setCipher(cipher);
        channel.pipeline().addLast(new UDPReplayCodec(config));
        String host = "192.168.255.1";
        String message = TestDice.rollString();
        InetSocketAddress dst = new InetSocketAddress(host, port);
        TernaryDatagramPacket noRelayPacket = new TernaryDatagramPacket(new DatagramPacket(Unpooled.wrappedBuffer(message.getBytes()), dst), null);
        Assertions.assertThrows(EncoderException.class, () -> channel.writeOutbound(noRelayPacket));
        channel.writeOutbound(new TernaryDatagramPacket(new DatagramPacket(Unpooled.wrappedBuffer(message.getBytes()), dst), replay));
        DatagramPacket out = channel.readOutbound();
        channel.writeInbound(out);
        DatagramPacket in = channel.readInbound();
        Assertions.assertEquals(in.recipient(), dst);
        ByteBuf content = in.content();
        Assertions.assertEquals(message, content.readCharSequence(content.readableBytes(), StandardCharsets.UTF_8));
    }
}
