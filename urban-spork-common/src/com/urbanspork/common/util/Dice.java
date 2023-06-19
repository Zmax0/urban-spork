package com.urbanspork.common.util;

import java.util.concurrent.ThreadLocalRandom;

public interface Dice {

    static byte[] rollBytes(int length) {
        if (length == 0) {
            return new byte[0];
        }
        byte[] bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

}
