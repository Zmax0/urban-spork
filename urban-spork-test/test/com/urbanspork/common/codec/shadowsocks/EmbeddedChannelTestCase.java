package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@DisplayName("Shadowsocks - Embedded Channel")
class EmbeddedChannelTestCase {

    @Test
    void testTCPReplayChannel() {
        int port = TestDice.rollPort();
        String host = TestDice.rollHost();
        DefaultSocks5CommandRequest request = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, host, port);
        SupportedCipher cipher = TestDice.rollCipher();
        String password = TestDice.rollString();
        EmbeddedChannel client = new EmbeddedChannel();
        client.pipeline().addLast(new TCPReplayCodec(StreamType.Client, request, password, cipher));
        EmbeddedChannel server = new EmbeddedChannel();
        server.pipeline().addLast(new TCPReplayCodec(StreamType.Server, password, cipher));
        String message = TestDice.rollString();
        client.writeOutbound(Unpooled.wrappedBuffer(message.getBytes()));
        ByteBuf msg = client.readOutbound();
        server.writeInbound(msg);
        InetSocketAddress address = server.readInbound();
        Assertions.assertEquals(address.getPort(), port);
        msg = server.readInbound();
        server.writeOutbound(msg);
        msg = server.readOutbound();
        client.writeInbound(msg);
        msg = client.readInbound();
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
