package com.urbanspork.common.codec;

import java.util.concurrent.ThreadLocalRandom;

public interface CipherCodec {

    static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

}
