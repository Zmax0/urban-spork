package com.urbanspork.common.codec;

public enum EmptyPaddingLengthGenerator implements PaddingLengthGenerator {

    INSTANCE;

    @Override
    public int nextPaddingLength() {
        return 0;
    }
}
