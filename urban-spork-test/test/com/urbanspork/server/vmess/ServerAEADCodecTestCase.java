package com.urbanspork.server.vmess;

import com.urbanspork.client.vmess.ClientAEADCodec;
import com.urbanspork.client.vmess.ClientAEADCodecTestCase;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

@DisplayName("VMess - Server AEAD Codec")
class ServerAEADCodecTestCase {

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
        ClientAEADCodec clientCodec = new ClientAEADCodec(CipherKind.aes_128_gcm, ADDRESS, UUID);
        ByteBuf buf = readOutbound(clientCodec);
        ServerConfig config = new ServerConfig();
        config.setPassword(java.util.UUID.randomUUID().toString());
        ServerAEADCodec serverCodec = new ServerAEADCodec(config);
        EmbeddedChannel serverChannel = new EmbeddedChannel();
        serverChannel.pipeline().addLast(serverCodec);
        Assertions.assertThrows(DecoderException.class, () -> serverChannel.writeInbound(buf));
    }

    @Test
    void testInvalidRequest() {
        RequestHeader header = RequestHeader.defaultHeader(SecurityType.AES128_GCM, new RequestCommand((byte) 100), ADDRESS, UUID);
        ClientAEADCodec clientCodec = ClientAEADCodecTestCase.codec(header, new ClientSession());
        ByteBuf buf = readOutbound(clientCodec);
        ServerConfig config = new ServerConfig();
        config.setPassword(UUID);
        ServerAEADCodec serverCodec = new ServerAEADCodec(config);
        EmbeddedChannel serverChannel = new EmbeddedChannel();
        serverChannel.pipeline().addLast(serverCodec);
        Assertions.assertThrows(DecoderException.class, () -> serverChannel.writeInbound(buf));
    }

    private static ByteBuf readOutbound(ClientAEADCodec clientCodec) {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(clientCodec);
        channel.writeOutbound(Unpooled.EMPTY_BUFFER);
        return channel.readOutbound();
    }

    private static EmbeddedChannel serverChannel() {
        ServerConfig config = new ServerConfig();
        config.setPassword(UUID);
        return (EmbeddedChannel) new EmbeddedChannel().pipeline().addLast(new ServerAEADCodec(config)).channel();
    }
}
