package com.urbanspork.client.vmess;

import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import com.urbanspork.common.protocol.vmess.encoding.SessionTestCase;
import com.urbanspork.common.protocol.vmess.header.RequestHeader;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import com.urbanspork.server.vmess.ServerAEADCodec;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@DisplayName("VMess - Client AEAD Codec")
public class ClientAEADCodecTestCase {

    @Test
    void testDecodeDifferentSession() throws Exception {
        String uuid = java.util.UUID.randomUUID().toString();
        Socks5CommandRequest address = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, "www.urban-spork.com", TestDice.randomPort());
        ChannelHandlerContext ctx = new EmbeddedChannel().pipeline().firstContext();
        RequestHeader header = RequestHeader.defaultHeader(SecurityType.AES128_GCM, address, uuid);
        ClientSession session1 = new ClientSession();
        ClientSession session2 = SessionTestCase.another(session1);
        ClientAEADCodec clientCodec1 = new ClientAEADCodec(header, session1);
        ClientAEADCodec clientCodec2 = new ClientAEADCodec(header, session2);
        ServerAEADCodec serverCodec = new ServerAEADCodec(new String[]{uuid});
        ByteBuf msg = Unpooled.wrappedBuffer(TestDice.randomString().getBytes());
        clientCodec1.encode(ctx, msg, Unpooled.buffer());
        ByteBuf buf = Unpooled.buffer();
        clientCodec2.encode(ctx, msg, buf);
        List<Object> list = new ArrayList<>();
        serverCodec.decode(ctx, buf, list);
        serverCodec.encode(ctx, msg, buf);
        ByteBuf slice = buf.readSlice(ThreadLocalRandom.current().nextInt(19, 34));
        int readerIndex = slice.readerIndex();
        clientCodec1.decode(ctx, slice, list);
        Assertions.assertEquals(readerIndex, slice.readerIndex());
        ByteBuf in = Unpooled.wrappedBuffer(slice, buf);
        Assertions.assertThrows(DecoderException.class, () -> clientCodec1.decode(ctx, in, list));
    }

    public static ClientAEADCodec codec(RequestHeader header, ClientSession session) {
        return new ClientAEADCodec(header, session);
    }
}
