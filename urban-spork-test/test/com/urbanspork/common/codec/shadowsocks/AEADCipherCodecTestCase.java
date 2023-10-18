package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.protocol.shadowsocks.RequestHeader;
import com.urbanspork.common.protocol.shadowsocks.StreamType;
import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@DisplayName("Shadowsocks - AEAD Cipher Codec")
@TestInstance(Lifecycle.PER_CLASS)
class AEADCipherCodecTestCase {

    private String password;
    private byte[] in;
    private final int maxChunkSize = 0xffff;

    @BeforeAll
    void beforeAll() {
        password = TestDice.rollString();
        in = Dice.rollBytes(maxChunkSize * 10);
    }

    @DisplayName("Single supported cipher repeat")
    @RepeatedTest(10)
    void repeatedTest() throws Exception {
        parameterizedTest(SupportedCipher.aes_128_gcm);
    }

    @ParameterizedTest
    @DisplayName("All supported cipher iterate")
    @EnumSource(SupportedCipher.class)
    void parameterizedTest(SupportedCipher cipher) throws Exception {
        int port = TestDice.rollPort();
        String host = TestDice.rollHost();
        DefaultSocks5CommandRequest request = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, host, port);
        AEADCipherCodec codec = AEADCipherCodecs.get(password, cipher);
        cipherTest(codec, new RequestHeader(Network.TCP, StreamType.Request, request), new RequestHeader(Network.TCP, StreamType.Response, null), true);
        cipherTest(codec, new RequestHeader(Network.UDP, StreamType.Request, request), new RequestHeader(Network.UDP, StreamType.Response, null), false);
    }


    private void cipherTest(AEADCipherCodec cipher, RequestHeader client, RequestHeader server, boolean reRoll) throws Exception {
        List<Object> list = cipherTest(cipher, client, server, Unpooled.copiedBuffer(in), reRoll);
        byte[] out = new byte[in.length];
        int len = 0;
        for (Object obj : list) {
            if (obj instanceof ByteBuf outBuf) {
                int readableBytes = outBuf.readableBytes();
                outBuf.readBytes(out, len, readableBytes);
                outBuf.release();
                len += readableBytes;
            }
        }
        Assertions.assertArrayEquals(in, out);
    }

    private List<Object> cipherTest(AEADCipherCodec cipher, RequestHeader client, RequestHeader server, ByteBuf in, boolean reRoll) throws Exception {
        List<ByteBuf> encodeSlices = new ArrayList<>();
        for (ByteBuf slice : randomSlice(in, false)) {
            ByteBuf buf = Unpooled.buffer();
            cipher.encode(client, slice, buf);
            encodeSlices.add(buf);
        }
        List<Object> out = new ArrayList<>();
        if (reRoll) {
            ByteBuf buffer = Unpooled.buffer();
            for (ByteBuf slice : randomSlice(merge(encodeSlices), true)) {
                buffer.writeBytes(slice);
                cipher.decode(server, buffer, out);
            }
        } else {
            for (ByteBuf buf : encodeSlices) {
                cipher.decode(server, buf, out);
            }
        }
        return out;
    }

    private List<ByteBuf> randomSlice(ByteBuf src, boolean firstSmallSlice) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<ByteBuf> list = new ArrayList<>();
        if (firstSmallSlice) {
            list.add(src.readSlice(Math.min(src.readableBytes(), random.nextInt(1024, 10240))));
        }
        while (src.isReadable()) {
            list.add(src.readSlice(Math.min(src.readableBytes(), random.nextInt(maxChunkSize))));
        }
        return list;
    }

    private ByteBuf merge(List<ByteBuf> list) {
        ByteBuf merged = Unpooled.buffer();
        for (ByteBuf buf : list) {
            merged.writeBytes(buf);
        }
        return merged;
    }
}