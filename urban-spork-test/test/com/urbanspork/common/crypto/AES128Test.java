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
        Assertions.assertEquals(str, new String(AES.decrypt(key, AES.encrypt(key, str.getBytes(), 16), 16)));
    }

    @Test
    void testEncryptAndDecryptInPlace() {
        byte[] key = Dice.rollBytes(16);
        String str = TestDice.rollString(16);
        byte[] in = str.getBytes();
        AES.encrypt(key, in, 16, in);
        Assertions.assertNotEquals(str, new String(in));
        AES.decrypt(key, in, 16, in);
        Assertions.assertEquals(str, new String(in));
    }

    @Test
    void testAes256() {
        byte[] decrypt = AES.decrypt("4ylXkB2KedlvbLFytehZISl HZNo3s3LR049qziLBO9YVsZB".getBytes(), Base64.getDecoder().decode("P1RKHzOxcv1GKRlbD5OZGA=="), 32);
        Assertions.assertEquals("Tp5MsnjQk/37dPkQpaBB9w==", Base64.getEncoder().encodeToString(decrypt));
    }

}