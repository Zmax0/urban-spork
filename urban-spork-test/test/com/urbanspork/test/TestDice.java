package com.urbanspork.test;

import com.urbanspork.common.codec.SupportedCipher;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public interface TestDice {

    static String randomString() {
        String str = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-=_+";
        Random random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            sb.append(str.charAt(random.nextInt(str.length())));
        }
        return sb.toString();
    }

    static SupportedCipher randomCipher() {
        SupportedCipher[] ciphers = SupportedCipher.values();
        return ciphers[ThreadLocalRandom.current().nextInt(0, ciphers.length)];
    }

    static int randomPort() {
        return ThreadLocalRandom.current().nextInt(0, 65535);
    }
}
