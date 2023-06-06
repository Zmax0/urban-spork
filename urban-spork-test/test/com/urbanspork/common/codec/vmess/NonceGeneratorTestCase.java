package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.util.Dice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        byte[] bytes = Dice.randomBytes(12);
        byte[] nonce = bytes.clone();
        NonceGenerator.generateStaticBytes(nonce).generate();
        Assertions.assertArrayEquals(bytes, nonce);
    }
}
