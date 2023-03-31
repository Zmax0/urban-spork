package com.urbanspork.common.protocol.vmess;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;

public interface VMess {

    byte VERSION = 1;
    byte[] KDF_SALT_AUTH_ID_ENCRYPTION_KEY = "AES Auth ID Encryption".getBytes();
    byte[] KDF_SALT_VMESS_AEAD_KDF = "VMess AEAD KDF".getBytes();
    byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_KEY = "VMess Header AEAD Key_Length".getBytes();
    byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_LENGTH_AEAD_IV = "VMess Header AEAD Nonce_Length".getBytes();
    byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_KEY = "VMess Header AEAD Key".getBytes();
    byte[] KDF_SALT_VMESS_HEADER_PAYLOAD_AEAD_IV = "VMess Header AEAD Nonce".getBytes();
    byte[] KDF_SALT_AEAD_RESP_HEADER_LEN_KEY = "AEAD Resp Header Len Key".getBytes();
    byte[] KDF_SALT_AEAD_RESP_HEADER_LEN_IV = "AEAD Resp Header Len IV".getBytes();
    byte[] KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_KEY = "AEAD Resp Header Key".getBytes();
    byte[] KDF_SALT_AEAD_RESP_HEADER_PAYLOAD_IV = "AEAD Resp Header IV".getBytes();

    static long timestamp(int delta) {
        int rangeInDelta = ThreadLocalRandom.current().nextInt(delta * 2) - delta;
        return Instant.now().getEpochSecond() + rangeInDelta;
    }

    static long crc32(byte[] bytes) {
        CRC32 checksum = new CRC32();
        checksum.update(bytes);
        return checksum.getValue();
    }

}
