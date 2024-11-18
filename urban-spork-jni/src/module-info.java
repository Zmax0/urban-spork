module com.urbanspork.jni {
    requires io.questdb.jar.jni;

    exports com.urbanspork.jni;
    exports com.urbanspork.jni.chacha8poly1305;
    exports com.urbanspork.jni.xchacha8poly1305;
    exports com.urbanspork.jni.xchacha20poly1305;
}