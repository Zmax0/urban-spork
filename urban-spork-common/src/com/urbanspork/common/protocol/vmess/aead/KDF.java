package com.urbanspork.common.protocol.vmess.aead;

import com.urbanspork.common.crypto.Digests;
import com.urbanspork.common.util.Bytes;

import java.util.Arrays;

public class KDF {
    private KDF() {}

    private static final byte[] KDF_SALT_VMESS_AEAD_KDF = "VMess AEAD KDF".getBytes();
    private static final int HMAC_BLOCK_SIZE = 64;

    @FunctionalInterface
    private interface Hash {
        byte[] hash(byte[] input);
    }

    public static byte[] kdf(byte[] key, byte[]... paths) {
        Hash hash = Digests.sha256::hash;
        hash = hmac(hash, KDF_SALT_VMESS_AEAD_KDF);
        for (byte[] path : paths) {
            hash = hmac(hash, path);
        }
        return hash.hash(key);
    }

    public static byte[] kdf(byte[] key, int len, byte[]... paths) {
        return len >= 32 ? kdf(key, paths) : Arrays.copyOf(kdf(key, paths), len);
    }

    public static byte[] kdf16(byte[] key, byte[]... paths) {
        return kdf(key, 16, paths);
    }

    private static Hash hmac(Hash inner, byte[] key) {
        byte[] normalizedKey = key.length > HMAC_BLOCK_SIZE ? inner.hash(key) : key;
        byte[] ipad = new byte[HMAC_BLOCK_SIZE];
        byte[] opad = new byte[HMAC_BLOCK_SIZE];
        Arrays.fill(ipad, (byte) 0x36);
        Arrays.fill(opad, (byte) 0x5c);
        for (int i = 0; i < normalizedKey.length; i++) {
            ipad[i] ^= normalizedKey[i];
            opad[i] ^= normalizedKey[i];
        }
        return input -> inner.hash(Bytes.concat(opad, inner.hash(Bytes.concat(ipad, input))));
    }

}
