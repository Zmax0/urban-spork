package com.urbanspork.client.vmess;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import com.urbanspork.common.protocol.vmess.encoding.SessionTestCase;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.protocol.vmess.header.RequestHeader;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import com.urbanspork.common.util.Dice;
import com.urbanspork.server.vmess.ServerAeadCodec;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@DisplayName("VMess - Client AEAD Codec")
public class ClientAEADCodecTestCase {

    @Test
    void testDecodeDifferentSession() throws Exception {
        String uuid = java.util.UUID.randomUUID().toString();
        InetSocketAddress address = InetSocketAddress.createUnresolved("www.urban-spork.com", TestDice.rollPort());
        RequestHeader header = RequestHeader.defaultHeader(SecurityType.AES128_GCM, RequestCommand.TCP, address, uuid);
        ClientSession session1 = new ClientSession();
        ClientSession session2 = SessionTestCase.another(session1);
        ClientAEADCodec clientCodec1 = new ClientAEADCodec(header, session1);
        ClientAEADCodec clientCodec2 = new ClientAEADCodec(header, session2);
        ServerConfig config = new ServerConfig();
        config.setPassword(uuid);
        ServerAeadCodec serverCodec = new ServerAeadCodec(config);
        ByteBuf msg = Unpooled.wrappedBuffer(Dice.rollBytes(1024));
        clientCodec1.encode(null, msg, Unpooled.buffer());
        ByteBuf buf = Unpooled.buffer();
        clientCodec2.encode(null, msg, buf);
        List<Object> list = new ArrayList<>();
        serverCodec.decode(null, buf, list);
        serverCodec.encode(null, msg, buf);
        ByteBuf slice = buf.readSlice(ThreadLocalRandom.current().nextInt(19, 34));
        int readerIndex = slice.readerIndex();
        clientCodec1.decode(null, slice, list);
        Assertions.assertEquals(readerIndex, slice.readerIndex());
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(clientCodec1);
        ByteBuf in = Unpooled.wrappedBuffer(slice, buf).retain();
        Assertions.assertThrows(DecoderException.class, () -> channel.writeInbound(in));
    }

    public static ClientAEADCodec codec(RequestHeader header, ClientSession session) {
        return new ClientAEADCodec(header, session);
    }
}
