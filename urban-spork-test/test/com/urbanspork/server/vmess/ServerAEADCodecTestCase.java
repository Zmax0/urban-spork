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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;
import java.util.ArrayList;

@DisplayName("VMess - Server AEAD Codec")
class ServerAEADCodecTestCase {

    private static final String UUID = java.util.UUID.randomUUID().toString();
    private static final ServerAEADCodec SERVER_CODEC = new ServerAEADCodec(new String[]{UUID});
    private static final Socks5CommandRequest REQUEST = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, "www.urban-spork.com", TestDice.randomPort());
    private static final ChannelHandlerContext CTX = new EmbeddedChannel().pipeline().firstContext();

    @Test
    void testDecodeEmptyHeader() throws Exception {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(AuthID.createAuthID(ID.newID(UUID), VMess.timestamp(30)));
        SERVER_CODEC.decode(CTX, buf, new ArrayList<>());
        Assertions.assertTrue(buf.isReadable());
    }

    @Test
    void testDecodeNoMatchedAuthID() throws Exception {
        ClientAEADCodec clientCodec = new ClientAEADCodec(SupportedCipher.aes_128_gcm, REQUEST, UUID);
        ByteBuf buf = Unpooled.buffer();
        clientCodec.encode(CTX, Unpooled.EMPTY_BUFFER, buf);
        ServerAEADCodec serverCodec = new ServerAEADCodec(new String[]{java.util.UUID.randomUUID().toString()});
        ArrayList<Object> list = new ArrayList<>();
        Assertions.assertThrows(DecoderException.class, () -> serverCodec.decode(CTX, buf, list));
    }

    @Test
    void testDecodeUnsupportedCommand() throws InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        ClientSession session = new ClientSession();
        RequestHeader header = new RequestHeader(VMess.VERSION, RequestCommand.UDP, new RequestOption[]{RequestOption.AuthenticatedLength}, SecurityType.AES128_GCM, REQUEST, ID.newID(UUID));
        ClientAEADCodec clientCodec = ClientAEADCodecTestCase.codec(header, session);
        ByteBuf buf = Unpooled.buffer();
        clientCodec.encodeRequest(header, session, buf);
        ArrayList<Object> list = new ArrayList<>();
        Assertions.assertThrows(DecoderException.class, () -> SERVER_CODEC.decode(CTX, buf, list));
    }
}
