package com.urbanspork.common.crypto;

public final class Shake128 {

    private final byte[] input;
    private long offset;

    public Shake128(byte[] input) {
        this.input = input.clone();
    }

    public void squeeze(byte[] out) {
        com.urbanspork.jni.shake128.Shake128.squeeze(input, offset, out);
        offset += out.length;
    }
}
