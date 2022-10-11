package com.urbanspork.test;

import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.ShadowsocksCiphers;
import com.urbanspork.common.cipher.ShadowsocksKey;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
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

    @ParameterizedTest
    @EnumSource(ShadowsocksCiphers.class)
    void parameterizedTest(ShadowsocksCiphers ciphers) throws Exception {
        ShadowsocksCipher cipher = ciphers.newCipher();
        ByteBuf inBuf = Unpooled.copiedBuffer(in);
        ShadowsocksKey key = new ShadowsocksKey(password.getBytes(), cipher.getKeySize());
        ByteBuf outBuf = cipherTest(cipher, key, inBuf);
        byte[] out = new byte[in.length];
        outBuf.readBytes(out, 0, outBuf.readableBytes());
        outBuf.release();
        Assertions.assertArrayEquals(in, out);
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

    private ByteBuf cipherTest(ShadowsocksCipher cipher, ShadowsocksKey key, ByteBuf inBuf) throws Exception {
        CompositeByteBuf encrypt = Unpooled.compositeBuffer();
        for (ByteBuf inBuf0 : randomDivide(inBuf)) {
            ByteBuf encrypt0 = cipher.encryptCipher().encrypt(inBuf0, key.getEncoded());
            encrypt.addComponent(true, encrypt0);
        }
        List<ByteBuf> divided = randomDivide(encrypt.consolidate());
        CompositeByteBuf out = Unpooled.compositeBuffer();
        CompositeByteBuf temp = Unpooled.compositeBuffer();
        for (ByteBuf divided0 : divided) {
            temp.addComponent(true, divided0);
            List<ByteBuf> decrypt = cipher.decryptCipher().decrypt(temp, key.getEncoded());
            if (!decrypt.isEmpty()) {
                for (ByteBuf decrypt0 : decrypt) {
                    out.addComponent(true, decrypt0);
                }
            }
        }
        return out;
    }

    private List<ByteBuf> randomDivide(ByteBuf src) {
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