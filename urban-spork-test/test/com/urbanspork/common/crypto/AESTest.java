package com.urbanspork.common.crypto;

import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

class AESTest {
    @Test
    void testEncryptAndDecrypt() {
        byte[] key = Dice.rollBytes(16);
        String str = TestDice.rollString(16);
        byte[] block = str.getBytes();
        AES.encrypt128(key, block);
        Assertions.assertNotEquals(str, new String(block));
        AES.decrypt128(key, block);
        Assertions.assertEquals(str, new String(block));
    }

    @Test
    void testAes256() {
        byte[] key = Arrays.copyOf("4ylXkB2KedlvbLFytehZISl HZNo3s3LR049qziLBO9YVsZB".getBytes(), 32);
        byte[] block = Base64.getDecoder().decode("P1RKHzOxcv1GKRlbD5OZGA==");
        AES.decrypt256(key, block);
        Assertions.assertEquals("Tp5MsnjQk/37dPkQpaBB9w==", Base64.getEncoder().encodeToString(block));
    }
}
