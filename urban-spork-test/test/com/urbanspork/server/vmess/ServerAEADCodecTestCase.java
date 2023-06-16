package com.urbanspork.server.vmess;

import com.urbanspork.client.vmess.ClientAEADCodec;
import com.urbanspork.client.vmess.ClientAEADCodecTestCase;
import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.protocol.vmess.ID;
import com.urbanspork.common.protocol.vmess.VMess;
import com.urbanspork.common.protocol.vmess.aead.AuthID;
import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.protocol.vmess.header.RequestHeader;
import com.urbanspork.common.protocol.vmess.header.RequestOption;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VMess - Server AEAD Codec")
class ServerAEADCodecTestCase {

    private static final String UUID = java.util.UUID.randomUUID().toString();
    private static final Socks5CommandRequest REQUEST = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, "www.urban-spork.com", TestDice.rollPort());

    @Test
    void testDecodeEmptyHeader() throws Exception {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(AuthID.createAuthID(ID.newID(UUID), VMess.timestamp(30)));
        serverChannel().writeInbound(buf);
        Assertions.assertTrue(buf.isReadable());
    }

    @Test
    void testDecodeNoMatchedAuthID() {
        ClientAEADCodec clientCodec = new ClientAEADCodec(SupportedCipher.aes_128_gcm, REQUEST, UUID);
        ByteBuf buf = readOutbound(clientCodec);
        ServerAEADCodec serverCodec = new ServerAEADCodec(new String[]{java.util.UUID.randomUUID().toString()});
        EmbeddedChannel serverChannel = new EmbeddedChannel();
        serverChannel.pipeline().addLast(serverCodec);
        Assertions.assertThrows(DecoderException.class, () -> serverChannel.writeInbound(buf));
    }

    @Test
    void testDecodeUnsupportedCommand() {
        ClientSession session = new ClientSession();
        RequestHeader header = new RequestHeader(VMess.VERSION, RequestCommand.UDP,
            new RequestOption[]{RequestOption.AuthenticatedLength}, SecurityType.AES128_GCM, REQUEST, ID.newID(UUID));
        ClientAEADCodec clientCodec = ClientAEADCodecTestCase.codec(header, session);
        ByteBuf buf = readOutbound(clientCodec);
        EmbeddedChannel channel = serverChannel();
        Assertions.assertThrows(DecoderException.class, () -> channel.writeInbound(buf));
    }

    private static ByteBuf readOutbound(ClientAEADCodec clientCodec) {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(clientCodec);
        channel.writeOutbound(Unpooled.EMPTY_BUFFER);
        return channel.readOutbound();
    }

    private static EmbeddedChannel serverChannel() {
        return (EmbeddedChannel) new EmbeddedChannel().pipeline().addLast(new ServerAEADCodec(new String[]{UUID})).channel();
    }
}
