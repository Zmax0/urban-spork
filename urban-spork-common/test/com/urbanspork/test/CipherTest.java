package com.urbanspork.test;

import java.security.SecureRandom;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.ShadowsocksCiphers;
import com.urbanspork.common.cipher.ShadowsocksKey;

@DisplayName("Cipher Test")
@TestInstance(Lifecycle.PER_CLASS)
public class CipherTest {

    private static final String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private String password;
    private ShadowsocksCipher cipher;
    private byte[] in;
    private byte[] out;

    @BeforeAll
    public void beforeAll() {
        password = randomString(str.length());
        in = randomBytes(new SecureRandom().nextInt(100));
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

    private static String randomString(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private void cipherTest() throws Exception {
        ShadowsocksKey key = new ShadowsocksKey(password, cipher.getKeySize());
        out = cipherTest(cipher, key, in);
    }

    private byte[] cipherTest(ShadowsocksCipher cipher, ShadowsocksKey key, byte[] in) throws Exception {
        byte[] encrypt = cipher.encrypt(in, key);
        byte[] subpackage0 = new byte[5];
        byte[] subpackage1 = new byte[15];
        byte[] subpackage2 = new byte[encrypt.length - 20];
        System.arraycopy(encrypt, 0, subpackage0, 0, subpackage0.length);
        System.arraycopy(encrypt, subpackage0.length, subpackage1, 0, subpackage1.length);
        System.arraycopy(encrypt, subpackage0.length + subpackage1.length, subpackage2, 0, subpackage2.length);
        byte[] decrypt0 = cipher.decrypt(subpackage0, key);
        byte[] decrypt1 = cipher.decrypt(subpackage1, key);
        byte[] decrypt2 = cipher.decrypt(subpackage2, key);
        byte[] decrypt = new byte[decrypt0.length + decrypt1.length + decrypt2.length];
        System.arraycopy(decrypt0, 0, decrypt, 0, decrypt0.length);
        System.arraycopy(decrypt1, 0, decrypt, decrypt0.length, decrypt1.length);
        System.arraycopy(decrypt2, 0, decrypt, decrypt0.length + decrypt1.length, decrypt2.length);
        return decrypt;
    }

}
