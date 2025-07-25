package com.urbanspork.jni;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

@DisplayName("jni.AeadCipherTest")
class AeadCipherTest {
    private static final byte[] KEY = Base64.getDecoder().decode("1nFNRRLWhYe4Tq2yBimnQTNMr0RIDhgwqZMRCWSoFcc=");

    @Test
    void testChaCha8Poly1305() {
        com.urbanspork.jni.chacha8poly1305.Cipher cipher = com.urbanspork.jni.chacha8poly1305.Cipher.newInstance(KEY);
        testEncrypt(cipher, 12, "DvAxs1hCeM+VySozlDhrdGewiF/WHljuRA==");
        testDecrypt(cipher, 12, "HfUgslNEadKZLZkH8tfHEif6qmmM54Pv6l4=");
    }

    @Test
    void testXChaCha8Poly1305() {
        com.urbanspork.jni.xchacha8poly1305.Cipher cipher = com.urbanspork.jni.xchacha8poly1305.Cipher.newInstance(KEY);
        testEncrypt(cipher, 24, "W1eIELrnJAOWgI2ab/A3tOewRWpeOzr4KA==");
        testDecrypt(cipher, 24, "SFKZEbHhNR6afVbK5ydKTg3cmPY9j5LQlqg=");
    }

    @Test
    void testXChaCha20Poly1305() {
        com.urbanspork.jni.xchacha20poly1305.Cipher cipher = com.urbanspork.jni.xchacha20poly1305.Cipher.newInstance(KEY);
        testEncrypt(cipher, 24, "K3qamUguu+ZoQ7SDEaYLRD8wHoeiIs58qA==");
        testDecrypt(cipher, 24, "OH+LmEMoqvtkQXzVc6bDnFV1xKzo7nLKKEs=");
    }

    void testEncrypt(AeadCipher cipher, int nonceSize, String dst) {
        byte[] bytes = "plaintext".getBytes();
        byte[] buffer = Arrays.copyOf(bytes, bytes.length + 16);
        byte[] nonce = new byte[nonceSize];
        cipher.encrypt(nonce, null, buffer);
        Assertions.assertEquals(dst, Base64.getEncoder().encodeToString(buffer));
    }

    void testDecrypt(AeadCipher cipher, int nonceSize, String src) {
        byte[] buffer = Base64.getDecoder().decode(src);
        byte[] nonce = new byte[nonceSize];
        cipher.decrypt(nonce, null, buffer);
        byte[] ciphertext = Arrays.copyOf(buffer, buffer.length - 16);
        Assertions.assertArrayEquals("ciphertext".getBytes(), ciphertext);
    }
}
