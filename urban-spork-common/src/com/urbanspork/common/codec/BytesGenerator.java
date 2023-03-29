package com.urbanspork.common.codec;

public interface BytesGenerator {

    byte[] generate();

    static BytesGenerator generateEmptyBytes() {
        return () -> null;
    }

}
