package com.urbanspork.common.util;

import java.util.concurrent.ThreadLocalRandom;

public interface Dice {

    byte[] EMPTY = new byte[0];

    static byte[] rollBytes(int length) {
        if (length <= 0) {
            return EMPTY;
        }
        byte[] bytes = new byte[length];
        rollBytes(bytes);
        return bytes;
    }

    static void rollBytes(byte[] btyes) {
        ThreadLocalRandom.current().nextBytes(btyes);
    }
}
