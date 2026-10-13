module com.urbanspork.jni {
    requires io.questdb.jar.jni;

    exports com.urbanspork.jni;
    exports com.urbanspork.jni.aes128;
    exports com.urbanspork.jni.aes128gcm;
    exports com.urbanspork.jni.aes256;
    exports com.urbanspork.jni.aes256gcm;
    exports com.urbanspork.jni.chacha20poly1305;
    exports com.urbanspork.jni.chacha8poly1305;
    exports com.urbanspork.jni.xchacha8poly1305;
    exports com.urbanspork.jni.xchacha20poly1305;
}
