package com.urbanspork.test;

import java.security.SecureRandom;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.urbanspork.cipher.ShadowsocksCipher;
import com.urbanspork.cipher.ShadowsocksCiphers;
import com.urbanspork.cipher.ShadowsocksKey;

@DisplayName("Cipher Test")
@TestInstance(Lifecycle.PER_CLASS)
public class CipherTest {

    private String password;
    private byte[] in;
    private byte[] out;

    @BeforeAll
    public void beforeAll() {
        password = randomString(64);
        in = randomBytes(100);
    }

    @ParameterizedTest
    @EnumSource(ShadowsocksCiphers.class)
    public void start(ShadowsocksCiphers cipher) throws Exception {
        cipherTest(cipher.newShadowsocksCipher());
    }

    @AfterEach
    public void afterEach() {
        Assertions.assertArrayEquals(in, out);
    }

    private static String randomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private void cipherTest(ShadowsocksCipher cipher) throws Exception {
        ShadowsocksKey key = new ShadowsocksKey(password, cipher.getKeyLength());
        out = cipherTest(cipher, key, in);
    }

    private byte[] cipherTest(ShadowsocksCipher cipher, ShadowsocksKey key, byte[] in) throws Exception {
        byte[] encrypt = cipher.encrypt(in, key);
        byte[] subpackage0 = new byte[encrypt.length - 10];
        byte[] subpackage1 = new byte[10];
        System.arraycopy(encrypt, 0, subpackage0, 0, subpackage0.length);
        System.arraycopy(encrypt, subpackage0.length, subpackage1, 0, subpackage1.length);
        byte[] decrypt0 = cipher.decrypt(subpackage0, key);
        byte[] decrypt1 = cipher.decrypt(subpackage1, key);
        byte[] decrypt = new byte[decrypt0.length + decrypt1.length];
        System.arraycopy(decrypt0, 0, decrypt, 0, decrypt0.length);
        System.arraycopy(decrypt1, 0, decrypt, decrypt0.length, decrypt1.length);
        return decrypt;
    }
}
