package com.urbanspork.jni.blake3;

import com.urbanspork.jni.NativeLibrary;

public final class Blake3 {

    private Blake3() {}

    static {
        NativeLibrary.load();
    }

    public static void hash(byte[] input, byte[] out) {
        hash0(input, out);
    }

    public static void deriveKey(byte[] context, byte[] material, byte[] out) {
        deriveKey0(context, material, out);
    }

    private static native void hash0(byte[] input, byte[] out);

    private static native void deriveKey0(byte[] context, byte[] material, byte[] out);
}
