package com.urbanspork.common.util;

import java.util.concurrent.ThreadLocalRandom;

public interface Dice {

    static byte[] rollBytes(int length) {
        if (length <= 0) {
            return new byte[0];
        }
        byte[] bytes = new byte[length];
        rollBytes(bytes);
        return bytes;
    }

    static void rollBytes(byte[] btyes) {
        do {
            ThreadLocalRandom.current().nextBytes(btyes);
        } while (allZeros(btyes));
    }

    static boolean allZeros(byte[] bytes) {
        int sum = 0;
        for (byte b : bytes) {
            sum |= b;
        }
        return (sum == 0);
    }
}
