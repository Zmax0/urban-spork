package com.urbanspork.common.util;

import java.util.Arrays;

public interface Bytes {
    static byte[] concat(byte[] bytes1, byte[] bytes2) {
        byte[] result = Arrays.copyOf(bytes1, bytes1.length + bytes2.length);
        System.arraycopy(bytes2, 0, result, bytes1.length, bytes2.length);
        return result;
    }
}
