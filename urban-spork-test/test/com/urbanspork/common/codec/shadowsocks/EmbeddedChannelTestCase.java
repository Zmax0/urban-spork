package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.shadowsocks.tcp.Context;
import com.urbanspork.common.codec.shadowsocks.tcp.TcpRelayCodec;
import com.urbanspork.common.codec.shadowsocks.udp.UdpRelayCodec;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.network.TernaryDatagramPacket;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@DisplayName("Shadowsocks - Embedded Channel")
class EmbeddedChannelTestCase {

    @Test
    void testTcpRelayChannel() {
        int port = TestDice.rollPort();
        String host = TestDice.rollHost();
        DefaultSocks5CommandRequest request = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, host, port);
        CipherKind cipher = TestDice.rollCipher();
        String password = TestDice.rollPassword(Protocols.shadowsocks, cipher);
        ServerConfig config = new ServerConfig();
        config.setCipher(cipher);
        config.setPassword(password);
        EmbeddedChannel client = new EmbeddedChannel();
        client.pipeline().addLast(new TcpRelayCodec(new Context(), config, request, Mode.Client));
        EmbeddedChannel server = new EmbeddedChannel();
        server.pipeline().addLast(new TcpRelayCodec(new Context(), config, Mode.Server));
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
    void testUdpRelayChannel() {
        EmbeddedChannel client = new EmbeddedChannel();
        EmbeddedChannel server = new EmbeddedChannel();
        CipherKind cipher = TestDice.rollCipher();
        int port = TestDice.rollPort();
        InetSocketAddress relay = new InetSocketAddress("192.168.1.1", port);
        ServerConfig config = new ServerConfig();
        config.setPassword(TestDice.rollPassword(Protocols.shadowsocks, cipher));
        config.setCipher(cipher);
        client.pipeline().addLast(new UdpRelayCodec(config, Mode.Client));
        server.pipeline().addLast(new UdpRelayCodec(config, Mode.Server));
        String host = "192.168.255.1";
        String message = TestDice.rollString();
        InetSocketAddress dst = new InetSocketAddress(host, port);
        TernaryDatagramPacket noRelayPacket = new TernaryDatagramPacket(new DatagramPacket(Unpooled.wrappedBuffer(message.getBytes()), dst), null);
        Assertions.assertThrows(EncoderException.class, () -> client.writeOutbound(noRelayPacket));
        client.writeOutbound(new TernaryDatagramPacket(new DatagramPacket(Unpooled.wrappedBuffer(message.getBytes()), dst), relay));
        DatagramPacket out = client.readOutbound();
        server.writeInbound(out);
        DatagramPacket in = server.readInbound();
        Assertions.assertEquals(in.recipient(), dst);
        ByteBuf content = in.content();
        Assertions.assertEquals(message, content.readCharSequence(content.readableBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void testAead2022UdpAntiReplay() {
        EmbeddedChannel server = new EmbeddedChannel();
        EmbeddedChannel client = new EmbeddedChannel();
        CipherKind kind = CipherKind.aead2022_blake3_aes_256_gcm;
        ServerConfig config = new ServerConfig();
        config.setPassword(TestDice.rollPassword(Protocols.shadowsocks, kind));
        config.setCipher(kind);
        client.pipeline().addLast(new UdpRelayCodec(config, Mode.Client));
        server.pipeline().addLast(new UdpRelayCodec(config, Mode.Server));
        InetSocketAddress sender = new InetSocketAddress(InetAddress.getLoopbackAddress(), 16801);
        InetSocketAddress recipient = new InetSocketAddress(InetAddress.getLoopbackAddress(), 16802);
        InetSocketAddress porxy = new InetSocketAddress(InetAddress.getLoopbackAddress(), 16803);
        DatagramPacket packet = new DatagramPacket(Unpooled.wrappedBuffer(Dice.rollBytes(10)), recipient, sender);
        client.writeOutbound(new TernaryDatagramPacket(packet, porxy));
        DatagramPacket outbound = client.readOutbound();
        server.writeInbound(outbound.copy());
        Assertions.assertNotNull(server.readInbound());
        server.writeInbound(outbound);
        Assertions.assertNull(server.readInbound());
    }

    @Test
    void testAead2022TcpAntiReplay() {
        EmbeddedChannel server1 = new EmbeddedChannel();
        EmbeddedChannel server2 = new EmbeddedChannel();
        EmbeddedChannel client = new EmbeddedChannel();
        CipherKind kind = CipherKind.aead2022_blake3_aes_256_gcm;
        ServerConfig config = new ServerConfig();
        config.setPassword(TestDice.rollPassword(Protocols.shadowsocks, kind));
        config.setCipher(kind);
        Context context = Context.checkReplay();
        server1.pipeline().addLast(new TcpRelayCodec(context, config, Mode.Server));
        server2.pipeline().addLast(new TcpRelayCodec(context, config, Mode.Server));
        DefaultSocks5CommandRequest request = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, "localhost", 16800);
        client.pipeline().addLast(new TcpRelayCodec(context, config, request, Mode.Client));
        client.writeOutbound(Unpooled.wrappedBuffer(Dice.rollBytes(10)));
        ByteBuf msg = client.readOutbound();
        server1.writeInbound(msg.copy());
        Assertions.assertThrows(DecoderException.class, () -> server2.writeInbound(msg));
    }
}
