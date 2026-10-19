package com.urbanspork.common.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public enum Digests {

    md5("MD5", 16),
    sha224("SHA-224", 28),
    sha256("SHA-256", 32),
    blake3(null, Blake3.OUT_LEN),
    ;

    private final String algorithm;
    private final int size;

    Digests(String algorithm, int size) {
        this.algorithm = algorithm;
        this.size = size;
    }

    public byte[] hash(byte[] in) {
        if (algorithm == null) {
            return Blake3.hash(in);
        }
        return digest().digest(in);
    }

    public void hash(byte[] in, byte[] out, int outOff) {
        if (algorithm == null) {
            byte[] hash = Blake3.hash(in);
            System.arraycopy(hash, 0, out, outOff, hash.length);
            return;
        }
        byte[] hash = digest().digest(in);
        System.arraycopy(hash, 0, out, outOff, hash.length);
    }

    public int size() {
        return size;
    }

    private MessageDigest digest() {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " is not available", e);
        }
    }
}
