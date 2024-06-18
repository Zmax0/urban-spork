package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.protocol.vmess.ID;
import com.urbanspork.common.protocol.vmess.VMess;
import com.urbanspork.common.protocol.vmess.encoding.ClientSession;
import com.urbanspork.common.protocol.vmess.encoding.ServerSession;
import com.urbanspork.common.protocol.vmess.header.RequestCommand;
import com.urbanspork.common.protocol.vmess.header.RequestHeader;
import com.urbanspork.common.protocol.vmess.header.RequestOption;
import com.urbanspork.common.protocol.vmess.header.SecurityType;
import com.urbanspork.common.util.Dice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

class AeadBodyCodecTest {

    private final String uuid = UUID.randomUUID().toString();

    @ParameterizedTest
    @EnumSource(SecurityType.class)
    void testBySecurity(SecurityType security) throws InvalidCipherTextException {
        RequestHeader header = RequestHeader.defaultHeader(security, RequestCommand.TCP, null, UUID.randomUUID().toString());
        testByHeader(header);
    }

    @ParameterizedTest
    @ArgumentsSource(RequestOptionProvider.class)
    void testByOptionMask(int mask) throws InvalidCipherTextException {
        testByHeader(new RequestHeader(VMess.VERSION, RequestCommand.TCP, RequestOption.fromMask((byte) mask), SecurityType.CHACHA20_POLY1305, null, ID.newID(uuid)));
        testByHeader(new RequestHeader(VMess.VERSION, RequestCommand.UDP, RequestOption.fromMask((byte) mask), SecurityType.AES128_GCM, null, ID.newID(uuid)));
    }

    @ParameterizedTest
    @ArgumentsSource(RequestCommandProvider.class)
    void testByCommand(RequestCommand command) throws InvalidCipherTextException {
        RequestHeader header = RequestHeader.defaultHeader(SecurityType.CHACHA20_POLY1305, command, null, UUID.randomUUID().toString());
        testByHeader(header);
    }

    private static void testByHeader(RequestHeader header) throws InvalidCipherTextException {
        ClientSession clientSession = new ClientSession();
        ServerSession serverSession = new ServerSession(clientSession);
        PayloadEncoder clientBodyEncoder = AEADBodyCodec.getBodyEncoder(header, clientSession);
        PayloadDecoder serverBodyDecoder = AEADBodyCodec.getBodyDecoder(header, serverSession);
        PayloadEncoder serverBodyEncoder = AEADBodyCodec.getBodyEncoder(header, serverSession);
        PayloadDecoder clientBodyDecoder = AEADBodyCodec.getBodyDecoder(header, clientSession);
        ByteBuf out = Unpooled.buffer();
        List<Object> list = new ArrayList<>();
        byte[] bytes;
        if (RequestCommand.UDP.equals(header.command())) {
            bytes = Dice.rollBytes(ThreadLocalRandom.current().nextInt(clientBodyEncoder.payloadLimit() / 2));
            clientBodyEncoder.encodePacket(Unpooled.wrappedBuffer(bytes), out);
            serverBodyDecoder.decodePacket(out, list);
            serverBodyEncoder.encodePacket(merge(list), out);
            clientBodyDecoder.decodePacket(out, list);
        } else {
            bytes = Dice.rollBytes(ThreadLocalRandom.current().nextInt(10 * clientBodyEncoder.payloadLimit()));
            clientBodyEncoder.encodePayload(Unpooled.wrappedBuffer(bytes), out);
            serverBodyDecoder.decodePayload(out, list);
            serverBodyEncoder.encodePayload(merge(list), out);
            clientBodyDecoder.decodePayload(out, list);
        }
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

    private static class RequestCommandProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(new RequestCommand[]{RequestCommand.TCP, RequestCommand.UDP}).map(Arguments::of);
        }
    }

    private static class RequestOptionProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            int option1 = RequestOption.ChunkStream.getValue();
            int option2 = RequestOption.ChunkStream.getValue() | RequestOption.ChunkMasking.getValue();
            int option3 = RequestOption.ChunkStream.getValue() | RequestOption.GlobalPadding.getValue();
            int option4 = RequestOption.ChunkStream.getValue() | RequestOption.ChunkMasking.getValue() | RequestOption.GlobalPadding.getValue();
            int option5 = RequestOption.ChunkStream.getValue() | RequestOption.AuthenticatedLength.getValue();
            int option6 = RequestOption.ChunkStream.getValue() | RequestOption.GlobalPadding.getValue() | RequestOption.AuthenticatedLength.getValue();
            int option7 = RequestOption.ChunkStream.getValue() | RequestOption.ChunkMasking.getValue() | RequestOption.GlobalPadding.getValue() | RequestOption.AuthenticatedLength.getValue();
            return Arrays.stream(new int[]{option1, option2, option3, option4, option5, option6, option7}).mapToObj(Arguments::of);
        }
    }
}
