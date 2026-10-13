package com.urbanspork.common.crypto;

public class AES {
    public static final int BLOCK_SIZE = 16;

    private AES() {}

    public static void encrypt128(byte[] key, byte[] block) {
        try (com.urbanspork.jni.aes128.Cipher cipher = com.urbanspork.jni.aes128.Cipher.newInstance(key)) {
            cipher.encrypt(block);
        }
    }

    public static void decrypt128(byte[] key, byte[] block) {
        try (com.urbanspork.jni.aes128.Cipher cipher = com.urbanspork.jni.aes128.Cipher.newInstance(key)) {
            cipher.decrypt(block);
        }
    }

    public static void encrypt256(byte[] key, byte[] block) {
        try (com.urbanspork.jni.aes256.Cipher cipher = com.urbanspork.jni.aes256.Cipher.newInstance(key)) {
            cipher.encrypt(block);
        }
    }

    public static void decrypt256(byte[] key, byte[] block) {
        try (com.urbanspork.jni.aes256.Cipher cipher = com.urbanspork.jni.aes256.Cipher.newInstance(key)) {
            cipher.decrypt(block);
        }
    }
}
