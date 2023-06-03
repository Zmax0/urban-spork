package com.urbanspork.common.protocol.vmess;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;

public interface VMess {

    byte VERSION = 1;

    static long now() {
        return Instant.now().getEpochSecond();
    }

    static long timestamp(int delta) {
        int rangeInDelta = ThreadLocalRandom.current().nextInt(delta * 2) - delta;
        return now() + rangeInDelta;
    }

    static long crc32(byte[] bytes) {
        CRC32 checksum = new CRC32();
        checksum.update(bytes);
        return checksum.getValue();
    }

}
