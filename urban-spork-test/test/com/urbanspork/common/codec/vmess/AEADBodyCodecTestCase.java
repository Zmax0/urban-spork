package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import com.urbanspork.common.protocol.vmess.encoding.ServerSession;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@DisplayName("VMess - AEAD Body Codec")
class AEADBodyCodecTestCase {

    @ParameterizedTest
    @EnumSource(SecurityType.class)
    void testBySecurity(SecurityType security) throws InvalidCipherTextException {
        ClientSession clientSession = new ClientSession();
        ServerSession serverSession = new ServerSession(clientSession);
        AEADBodyEncoder clientBodyEncoder = AEADBodyCodec.getBodyEncoder(security, clientSession);
        AEADBodyDecoder serverBodyDecoder = AEADBodyCodec.getBodyDecoder(security, serverSession);
        AEADBodyEncoder serverBodyEncoder = AEADBodyCodec.getBodyEncoder(security, serverSession);
        AEADBodyDecoder clientBodyDecoder = AEADBodyCodec.getBodyDecoder(security, clientSession);
        byte[] bytes = Dice.randomBytes(ThreadLocalRandom.current().nextInt(10 * clientBodyEncoder.payloadLimit()));
        ByteBuf out = Unpooled.buffer();
        clientBodyEncoder.encodePayload(Unpooled.wrappedBuffer(bytes), out);
        List<Object> list = new ArrayList<>();
        serverBodyDecoder.decodePayload(out, list);
        serverBodyEncoder.encodePayload(merge(list), out);
        clientBodyDecoder.decodePayload(out, list);
        Assertions.assertArrayEquals(bytes, ByteBufUtil.getBytes(merge(list)));
    }

    private static ByteBuf merge(List<Object> list) {
        ByteBuf merged = Unpooled.buffer();
        for (Object obj : list) {
            if (obj instanceof ByteBuf out) {
                merged.writeBytes(out);
            }
        }
        list.clear();
        return merged;
    }
}
