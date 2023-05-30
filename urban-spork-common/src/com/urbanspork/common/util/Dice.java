package com.urbanspork.common.util;

import java.util.concurrent.ThreadLocalRandom;

public interface Dice {

    static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

}
