package com.urbanspork.test;

import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.ShadowsocksCiphers;
import com.urbanspork.common.cipher.ShadowsocksKey;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.security.SecureRandom;

@DisplayName("Cipher Test")
@TestInstance(Lifecycle.PER_CLASS)
public class CipherTest {

    private static final String str = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-=_+";

    private String password;
    private ShadowsocksCipher cipher;
    private byte[] in;
    private byte[] out;

    @BeforeAll
    public void beforeAll() {
        password = randomString();
        SecureRandom random = new SecureRandom();
        int length = random.nextInt(2048);
        in = new byte[length];
        random.nextBytes(in);
    }

    @ParameterizedTest
    @EnumSource(ShadowsocksCiphers.class)
    public void start(ShadowsocksCiphers cipher) throws Exception {
        this.cipher = cipher.newShadowsocksCipher();
        cipherTest();
    }

    @AfterEach
    public void afterEach() {
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

    private void cipherTest() throws Exception {
        ShadowsocksKey key = new ShadowsocksKey(password, cipher.getKeySize());
        out = cipherTest(cipher, key, in);
    }

    private byte[] cipherTest(ShadowsocksCipher cipher, ShadowsocksKey key, byte[] in) throws Exception {
        ByteBuf buff = Unpooled.directBuffer();
        byte[] temp = in;
        for (int i = 0; i < 5; i++) {
            byte[][] divided = randomDivide(temp);
            buff.writeBytes(cipher.encrypt(divided[0], key));
            temp = divided[1];
        }
        buff.writeBytes(cipher.encrypt(temp, key));
        byte[] encrypt = new byte[buff.readableBytes()];
        buff.readBytes(encrypt);
        temp = encrypt;
        for (int i = 0; i < 5; i++) {
            byte[][] divided = randomDivide(temp);
            buff.writeBytes(cipher.decrypt(divided[0], key));
            temp = divided[1];
        }
        buff.writeBytes(cipher.decrypt(temp, key));
        byte[] out = new byte[buff.readableBytes()];
        buff.readBytes(out);
        buff.release();
        return out;
    }

    private byte[][] randomDivide(byte[] src) {
        int i = new SecureRandom().nextInt(src.length);
        byte[] b1 = new byte[i];
        byte[] b2 = new byte[src.length - i];
        System.arraycopy(src, 0, b1, 0, i);
        System.arraycopy(src, i, b2, 0, src.length - i);
        byte[][] result = new byte[2][];
        result[0] = b1;
        result[1] = b2;
        return result;
    }

}
