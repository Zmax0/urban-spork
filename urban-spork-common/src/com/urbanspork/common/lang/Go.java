package com.urbanspork.common.lang;

import java.util.concurrent.ThreadLocalRandom;

public interface Go {

    static byte[] getUnsignedInt(long u32) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (u32 >> 24);
        bytes[1] = (byte) (u32 >> 16);
        bytes[2] = (byte) (u32 >> 8);
        bytes[3] = (byte) u32;
        return bytes;
    }

    static byte[] nextUnsignedInt() {
        return getUnsignedInt(Integer.toUnsignedLong(ThreadLocalRandom.current().nextInt()));
    }

}
