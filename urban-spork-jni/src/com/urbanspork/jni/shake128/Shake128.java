package com.urbanspork.jni.shake128;

import com.urbanspork.jni.NativeLibrary;

public final class Shake128 {

    private Shake128() {}

    static {
        NativeLibrary.load();
    }

    public static void squeeze(byte[] input, long offset, byte[] out) {
        squeeze0(input, offset, out);
    }

    private static native void squeeze0(byte[] input, long offset, byte[] out);
}
