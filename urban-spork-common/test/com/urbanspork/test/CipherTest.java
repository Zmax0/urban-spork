package com.urbanspork.test;

import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.ShadowsocksCiphers;
import com.urbanspork.common.cipher.ShadowsocksKey;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@DisplayName("Cipher Test")
@TestInstance(Lifecycle.PER_CLASS)
class CipherTest {

    private static final String str = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-=_+";

    private String password;
    private byte[] in;

    @BeforeAll
    public void beforeAll() {
        password = randomString();
        SecureRandom random = new SecureRandom();
        in = new byte[10485760]; // 10M
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
        cipherTest(ShadowsocksCiphers.defaultCipher().newCipher());
    }

    @ParameterizedTest
    @EnumSource(ShadowsocksCiphers.class)
    void parameterizedTest(ShadowsocksCiphers ciphers) throws Exception {
        cipherTest(ciphers.newCipher());
    }

    private void cipherTest(ShadowsocksCipher cipher) throws Exception {
        List<Object> list = cipherTest(cipher, new ShadowsocksKey(password.getBytes(), cipher.getKeySize()), Unpooled.copiedBuffer(in));
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

    private List<Object> cipherTest(ShadowsocksCipher cipher, ShadowsocksKey key, ByteBuf inBuf) throws Exception {
        ByteBuf encrypt = Unpooled.buffer();
        for (ByteBuf inBuf0 : randomSlice(inBuf)) {
            cipher.encryptCipher().encrypt(inBuf0, key.getEncoded(), encrypt);
        }
        inBuf.release();
        List<ByteBuf> sliced = randomSlice(encrypt);
        List<Object> out = new ArrayList<>();
        CompositeByteBuf temp = Unpooled.compositeBuffer();
        for (ByteBuf slice : sliced) {
            temp.addComponent(true, slice);
            cipher.decryptCipher().decrypt(temp, key.getEncoded(), out);
        }
        return out;
    }

    private List<ByteBuf> randomSlice(ByteBuf src) {
        SecureRandom random = new SecureRandom();
        int i = random.nextInt(10);
        List<ByteBuf> list = new ArrayList<>();
        for (int j = 1; j < i && src.readableBytes() > 1; j++) {
            int len = random.nextInt(src.readableBytes() - 1) + 1;
            ByteBuf buf = src.readSlice(len);
            list.add(buf);
        }
        if (src.isReadable()) {
            ByteBuf buf = src.readSlice(src.readableBytes());
            list.add(buf);
        }
        return list;
    }

}