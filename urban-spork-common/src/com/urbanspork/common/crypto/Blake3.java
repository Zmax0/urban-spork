package com.urbanspork.common.crypto;

public final class Blake3 {

    public static final int OUT_LEN = 32;

    private Blake3() {}

    public static byte[] hash(byte[] input) {
        byte[] out = new byte[OUT_LEN];
        com.urbanspork.jni.blake3.Blake3.hash(input, out);
        return out;
    }

    public static byte[] deriveKey(byte[] context, byte[] material, int length) {
        byte[] out = new byte[length];
        com.urbanspork.jni.blake3.Blake3.deriveKey(context, material, out);
        return out;
    }
}
