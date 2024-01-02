package com.urbanspork.common.crypto;

import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

@DisplayName("Common - AES")
class AESTestCase {
    @Test
    void testEncryptAndDecrypt() {
        byte[] key = Dice.rollBytes(16);
        String str = TestDice.rollString(16);
        Assertions.assertEquals(str, new String(AES.INSTANCE.decrypt(key, AES.INSTANCE.encrypt(key, str.getBytes()))));
    }

    @Test
    void testAes128() {
        byte[] decrypt = AES.INSTANCE.decrypt("4ylXkB2KedlvbLFy".getBytes(), Base64.getDecoder().decode("P1RKHzOxcv1GKRlbD5OZGA=="));
        Assertions.assertEquals("VqaYmC3G66ZuPB6J", new String(decrypt));
    }
}