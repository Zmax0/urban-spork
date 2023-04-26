package com.urbanspork.common.channel.shadowsocks;

import com.urbanspork.common.TestDice;
import com.urbanspork.common.channel.AttributeKeys;
import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.shadowsocks.ShadowsocksAEADCipherCodecs;
import com.urbanspork.common.codec.shadowsocks.ShadowsocksUDPReplayCodec;
import com.urbanspork.common.protocol.shadowsocks.ShadowsocksAddressDecoder;
import com.urbanspork.common.protocol.shadowsocks.ShadowsocksAddressEncoder;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

class ShadowsocksChannelTestCase {

    @ParameterizedTest
    @EnumSource(Network.class)
    void testCommonChannel(Network network) {
        int port = TestDice.randomPort();
        SupportedCipher cipher = TestDice.randomCipher();
        String password = TestDice.randomString();
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline()
                .addLast(ShadowsocksAEADCipherCodecs.get(password, cipher, network))
                .addLast(new ShadowsocksAddressEncoder(new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, "localhost", port)))
                .addLast(new ShadowsocksAddressDecoder());
        String message = TestDice.randomString();
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
        SupportedCipher cipher = TestDice.randomCipher();
        int port = TestDice.randomPort();
        channel.attr(AttributeKeys.REPLAY_ADDRESS).set(new InetSocketAddress("192.168.1.1", port));
        channel.pipeline().addLast(new ShadowsocksUDPReplayCodec(
                ShadowsocksAEADCipherCodecs.get(TestDice.randomString(), cipher, Network.UDP)));
        String host = "192.168.255.1";
        String message = TestDice.randomString();
        InetSocketAddress dst = new InetSocketAddress(host, port);
        channel.writeOutbound(new DatagramPacket(Unpooled.wrappedBuffer(message.getBytes()), dst));
        DatagramPacket out = channel.readOutbound();
        channel.writeInbound(out);
        DatagramPacket in = channel.readInbound();
        Assertions.assertEquals(in.recipient(), dst);
        ByteBuf content = in.content();
        Assertions.assertEquals(message, content.readCharSequence(content.readableBytes(), StandardCharsets.UTF_8));
    }
}
