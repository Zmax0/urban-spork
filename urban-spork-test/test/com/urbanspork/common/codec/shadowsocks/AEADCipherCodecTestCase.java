package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.test.TestDice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.security.SecureRandom;
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
        SecureRandom random = new SecureRandom();
        in = new byte[maxChunkSize * 10]; // 1M
        random.nextBytes(in);
    }

    @DisplayName("Single supported cipher repeat")
    @RepeatedTest(10)
    void repeatedTest() throws Exception {
        cipherTest(AEADCipherCodecs.get(password, SupportedCipher.aes_128_gcm, Network.TCP), true);
        cipherTest(AEADCipherCodecs.get(password, SupportedCipher.aes_128_gcm, Network.UDP), false);
    }

    @ParameterizedTest
    @DisplayName("All supported cipher iterate")
    @EnumSource(SupportedCipher.class)
    void parameterizedTest(SupportedCipher cipher) throws Exception {
        cipherTest(AEADCipherCodecs.get(password, cipher, Network.TCP), true);
        cipherTest(AEADCipherCodecs.get(password, cipher, Network.UDP), false);
    }

    private void cipherTest(AEADCipherCodec codec, boolean firstSmallSlice) throws Exception {
        List<Object> list = cipherTest(codec, Unpooled.copiedBuffer(in), firstSmallSlice);
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

    private List<Object> cipherTest(AEADCipherCodec codec, ByteBuf inBuf, boolean firstSmallSlice) throws Exception {
        ByteBuf trans = Unpooled.buffer();
        for (ByteBuf slice : randomSlice(inBuf, false)) {
            codec.encode(null, slice, trans);
        }
        List<Object> out = new ArrayList<>();
        ByteBuf buffer = Unpooled.buffer();
        for (ByteBuf slice : randomSlice(trans, firstSmallSlice)) {
            buffer.writeBytes(slice);
            codec.decode(null, buffer, out);
        }
        return out;
    }

    private List<ByteBuf> randomSlice(ByteBuf src, boolean firstSmallSlice) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<ByteBuf> list = new ArrayList<>();
        if (firstSmallSlice) {
            list.add(src.readSlice(Math.min(src.readableBytes(), random.nextInt(15))));
        }
        while (src.isReadable()) {
            list.add(src.readSlice(Math.max(src.readableBytes(), random.nextInt(1, maxChunkSize))));
        }
        return list;
    }
}