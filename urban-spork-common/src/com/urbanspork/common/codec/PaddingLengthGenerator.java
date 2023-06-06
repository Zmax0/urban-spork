package com.urbanspork.common.codec;

public interface PaddingLengthGenerator {

    int maxPaddingLength();

    int nextPaddingLength();
}
