package com.urbanspork.test;

import com.urbanspork.common.codec.SupportedCipher;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public interface TestDice {

    static String rollString(int length) {
        String str = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-=_+";
        Random random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(str.charAt(random.nextInt(str.length())));
        }
        return sb.toString();
    }

    static String rollString() {
        return rollString(256);
    }

    static SupportedCipher rollCipher() {
        SupportedCipher[] ciphers = SupportedCipher.values();
        return ciphers[ThreadLocalRandom.current().nextInt(0, ciphers.length)];
    }

    static int rollPort() {
        return ThreadLocalRandom.current().nextInt(49152, 65535);
    }
}
