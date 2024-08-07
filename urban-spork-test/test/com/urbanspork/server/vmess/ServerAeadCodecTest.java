package com.urbanspork.server.vmess;

import com.urbanspork.client.vmess.ClientAeadCodec;
import com.urbanspork.client.vmess.ClientAeadCodecTest;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.vmess.ID;
import com.urbanspork.common.protocol.vmess.VMess;
import com.urbanspork.common.protocol.vmess.aead.AuthID;
import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.protocol.vmess.header.RequestHeader;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

class ServerAeadCodecTest {

    private static final String UUID = java.util.UUID.randomUUID().toString();
    private static final InetSocketAddress ADDRESS = InetSocketAddress.createUnresolved("www.urban-spork.com", TestDice.rollPort());

    @Test
    void testDecodeEmptyHeader() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(AuthID.createAuthID(ID.newID(UUID), VMess.timestamp(30)));
        serverChannel().writeInbound(buf);
        Assertions.assertTrue(buf.isReadable());
    }

    @Test
    void testDecodeNoMatchedAuthID() {
        ClientAeadCodec clientCodec = new ClientAeadCodec(CipherKind.aes_128_gcm, ADDRESS, UUID);
        ByteBuf buf = readOutbound(clientCodec);
        ServerConfig config = new ServerConfig();
        config.setPassword(java.util.UUID.randomUUID().toString());
        ServerAeadCodec serverCodec = new ServerAeadCodec(config);
        EmbeddedChannel serverChannel = new EmbeddedChannel();
        serverChannel.pipeline().addLast(serverCodec);
        Assertions.assertThrows(DecoderException.class, () -> serverChannel.writeInbound(buf));
    }

    @Test
    void testInvalidRequest() {
        RequestHeader header = RequestHeader.defaultHeader(SecurityType.AES128_GCM, new RequestCommand((byte) 100), ADDRESS, UUID);
        ClientAeadCodec clientCodec = ClientAeadCodecTest.codec(header, new ClientSession());
        ByteBuf buf = readOutbound(clientCodec);
        ServerConfig config = new ServerConfig();
        config.setPassword(UUID);
        ServerAeadCodec serverCodec = new ServerAeadCodec(config);
        EmbeddedChannel serverChannel = new EmbeddedChannel();
        serverChannel.pipeline().addLast(serverCodec);
        Assertions.assertThrows(DecoderException.class, () -> serverChannel.writeInbound(buf));
    }

    private static ByteBuf readOutbound(ClientAeadCodec clientCodec) {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(clientCodec);
        channel.writeOutbound(Unpooled.EMPTY_BUFFER);
        return channel.readOutbound();
    }

    private static EmbeddedChannel serverChannel() {
        ServerConfig config = new ServerConfig();
        config.setPassword(UUID);
        return (EmbeddedChannel) new EmbeddedChannel().pipeline().addLast(new ServerAeadCodec(config)).channel();
    }
}
