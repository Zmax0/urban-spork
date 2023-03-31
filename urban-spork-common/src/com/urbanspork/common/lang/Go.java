package com.urbanspork.common.lang;

import java.util.concurrent.ThreadLocalRandom;

public interface Go {

    /**
     * 32 bits FNV-1a hash function using golang implementation
     *
     * @param data data
     * @return hash
     */
    static byte[] fnv1a32(byte[] data) {
        long hash = 2166136261L;
        for (byte b : data) {
            hash ^= Byte.toUnsignedInt(b);
            hash *= 16777619L;
        }
        hash = hash & 0xffffffffL;
        return getUnsignedInt(hash);
    }

    static byte[] nextUnsignedInt() {
        return getUnsignedInt(Integer.toUnsignedLong(ThreadLocalRandom.current().nextInt()));
    }

    static byte[] getUnsignedInt(long u32) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (u32 >> 24);
        bytes[1] = (byte) (u32 >> 16);
        bytes[2] = (byte) (u32 >> 8);
        bytes[3] = (byte) u32;
        return bytes;
    }
}
