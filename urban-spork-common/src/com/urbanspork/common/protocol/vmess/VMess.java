package com.urbanspork.common.protocol.vmess;

import com.urbanspork.common.lang.Go;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

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

    static byte[] md5(byte[] in) {
        MD5Digest digest = new MD5Digest();
        digest.update(in, 0, in.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

    static byte[] sha256(byte[] in) {
        SHA256Digest digest = new SHA256Digest();
        digest.update(in, 0, in.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

//    static byte[] shake(byte[] in) {
//        byte[] out = new byte[in.length];
//        SHAKEDigest digest = new SHAKEDigest();
//        digest.update(in, 0, in.length);
//        digest.doFinal(out, 0);
//        return out;
//    }

    /**
     * 32 bits FNV-1a hash function using golang implementation
     *
     * @param data data
     * @return hash
     */
    static byte[] fnv1a32(byte[] data) {
        long hash = 2166136261L;
        for (byte b : data) {
            hash ^= Byte.toUnsignedInt(b);
            hash *= 16777619L;
        }
        hash = hash & 0xffffffffL;
        return Go.getUnsignedInt(hash);
    }

    static long crc32(byte[] bytes) {
        CRC32 check = new CRC32();
        check.update(bytes);
        return check.getValue();
    }

}
