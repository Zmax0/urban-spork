package com.urbanspork.jni;

import io.questdb.jar.jni.JarJniLoader;

public final class NativeLibrary {

    private NativeLibrary() {}

    static {
        JarJniLoader.loadLib(
            NativeLibrary.class,
            "/native" /* match with <rust-maven-plugin.copyTo> in pom.xml */,
            "ciphers" /* match with [lib.name] in Cargo.toml */
        );
    }

    public static void load() {

    }
}
