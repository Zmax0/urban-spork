package com.urbanspork.common.crypto;

import com.urbanspork.common.util.Dice;
import com.urbanspork.test.TestDice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;

@DisplayName("Common - AES")
class AESTestCase {
    @ParameterizedTest
    @EnumSource(AES.class)
    void testEncryptAndDecrypt(AES aes) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] key = Dice.rollBytes(32);
        String str = TestDice.rollString();
        Assertions.assertEquals(str, new String(aes.decrypt(key, aes.encrypt(key, str.getBytes()))));
    }

    @Test
    void testGetCipher() {
        String transformation = TestDice.rollString(5);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> AES.getCipher(transformation));
    }
}