package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.util.Dice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

@DisplayName("VMess - Nonce Generator")
class NonceGeneratorTestCase {
    @Test
    void testGenerateIncreasingNonce() {
        byte[] nonce = new byte[]{(byte) 0xff, (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        NonceGenerator.generateIncreasingNonce(nonce).generate();
        Assertions.assertArrayEquals(new byte[]{0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, nonce);
    }

    @Test
    void testGenerateInitialAEADNonce() {
        byte[] nonce = NonceGenerator.generateInitialAEADNonce().generate();
        Assertions.assertArrayEquals(new byte[nonce.length], nonce);
    }

    @Test
    void testGenerateStaticBytes() {
        byte[] bytes = Dice.rollBytes(12);
        byte[] nonce = bytes.clone();
        NonceGenerator.generateStaticBytes(nonce).generate();
        Assertions.assertArrayEquals(bytes, nonce);
    }

    @Test
    void testGenerateCountingNonce() {
        byte[] nonce = new byte[12];
        NonceGenerator nonceGenerator = NonceGenerator.generateCountingNonce(nonce, nonce.length);
        byte[] generate = null;
        for (int i = 0; i < 65536; i++) {
            generate = nonceGenerator.generate();
        }
        Assertions.assertEquals("//8AAAAAAAAAAAAA", Base64.getEncoder().encodeToString(generate));
        Assertions.assertEquals("//8AAAAAAAAAAAAA", Base64.getEncoder().encodeToString(nonce));
    }
}
