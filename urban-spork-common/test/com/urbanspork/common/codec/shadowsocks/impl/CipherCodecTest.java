package com.urbanspork.common.codec.shadowsocks.impl;

import com.urbanspork.common.codec.EmptyChannelHandlerContext;
import com.urbanspork.common.codec.SupportedCipher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@TestInstance(Lifecycle.PER_CLASS)
class CipherCodecTest {

    private static final String str = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-=_+";

    private byte[] password;
    private byte[] in;

    private final int maxChunkSize = 65535;

    @BeforeAll
    public void beforeAll() {
        password = randomString().getBytes();
        SecureRandom random = new SecureRandom();
        in = new byte[maxChunkSize * 10]; // 1M
        random.nextBytes(in);
    }

    private static String randomString() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    @RepeatedTest(10)
    void repeatedTest() throws Exception {
        cipherTest(ShadowsocksCipherCodecs.get(SupportedCipher.aes_128_gcm, password));
    }

    @ParameterizedTest
    @EnumSource(SupportedCipher.class)
    void parameterizedTest(SupportedCipher cipher) throws Exception {
        cipherTest(ShadowsocksCipherCodecs.get(cipher, password));
    }

    private void cipherTest(ShadowsocksCipherCodec codec) throws Exception {
        List<Object> list = cipherTest(codec, Unpooled.copiedBuffer(in));
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

    private List<Object> cipherTest(ShadowsocksCipherCodec codec, ByteBuf inBuf) throws Exception {
        ByteBuf trans = Unpooled.buffer();
        for (ByteBuf slice : randomSlice(inBuf)) {
            codec.encode(EmptyChannelHandlerContext.INSTANCE, slice, trans);
        }
        List<Object> out = new ArrayList<>();
        ByteBuf buffer = Unpooled.buffer();
        for (ByteBuf slice : randomSlice(trans)) {
            buffer.writeBytes(slice);
            codec.decode(EmptyChannelHandlerContext.INSTANCE, buffer, out);
        }
        return out;
    }

    private List<ByteBuf> randomSlice(ByteBuf src) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<ByteBuf> list = new ArrayList<>();
        while (src.isReadable()) {
            list.add(src.readSlice(Math.max(src.readableBytes(), random.nextInt(1, maxChunkSize))));
        }
        return list;
    }

}