package com.urbanspork.test;

import java.security.SecureRandom;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.urbanspork.cipher.ShadowsocksCipher;
import com.urbanspork.cipher.ShadowsocksCiphers;
import com.urbanspork.cipher.ShadowsocksKey;

@DisplayName("Cipher Test")
public class CipherTest {

    @DisplayName("<= aes_256_cfb")
    @Test
    public void aes_256_cfb() throws Exception {
        ShadowsocksCipher cipher = ShadowsocksCiphers.AES_256_CFB.get();
        ShadowsocksKey key = new ShadowsocksKey(randomString(64), cipher.getKeyLength());
        byte[] in = randomBytes(100);
        byte[] out = test(cipher, key, in);
        Assertions.assertArrayEquals(in, out);
    }

    @DisplayName("<= aes-256-gcm")
    @Test
    public void aes_256_gcm() throws Exception {
        ShadowsocksCipher cipher = ShadowsocksCiphers.AES_256_GCM.get();
        ShadowsocksKey key = new ShadowsocksKey(randomString(64), cipher.getKeyLength());
        byte[] in = randomBytes(100);
        byte[] out = test(cipher, key, in);
        Assertions.assertArrayEquals(in, out);
    }

    @DisplayName("<= chacha20-ietf")
    @Test
    public void chacha20_ietf() throws Exception {
        ShadowsocksCipher cipher = ShadowsocksCiphers.ChaCha20_IETF.get();
        ShadowsocksKey key = new ShadowsocksKey(randomString(64), cipher.getKeyLength());
        byte[] in = randomBytes(100);
        byte[] out = test(cipher, key, in);
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

    private byte[] test(ShadowsocksCipher cipher, ShadowsocksKey key, byte[] in) throws Exception {
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
