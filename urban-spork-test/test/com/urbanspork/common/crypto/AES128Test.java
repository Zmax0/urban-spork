package com.urbanspork.common.crypto;

import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

class AESTest {
    @Test
    void testEncryptAndDecrypt() {
        byte[] key = Dice.rollBytes(16);
        String str = TestDice.rollString(16);
        Assertions.assertEquals(str, new String(AES.decrypt(key, AES.encrypt(key, str.getBytes()))));
    }

    @Test
    void testEncryptAndDecryptInPlace() {
        byte[] key = Dice.rollBytes(16);
        String str = TestDice.rollString(16);
        byte[] in = str.getBytes();
        AES.encrypt(key, in, in);
        Assertions.assertNotEquals(str, new String(in));
        AES.decrypt(key, in, in);
        Assertions.assertEquals(str, new String(in));
    }

    @Test
    void testAes128() {
        byte[] decrypt = AES.decrypt("4ylXkB2KedlvbLFy".getBytes(), Base64.getDecoder().decode("P1RKHzOxcv1GKRlbD5OZGA=="));
        Assertions.assertEquals("VqaYmC3G66ZuPB6J", new String(decrypt));
    }
}