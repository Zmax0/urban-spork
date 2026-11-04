package com.urbanspork.common.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public enum Digests {

    md5("MD5", 16),
    sha224("SHA-224", 28),
    sha256("SHA-256", 32),
    ;

    private final String algorithm;
    private final int size;

    Digests(String algorithm, int size) {
        this.algorithm = algorithm;
        this.size = size;
    }

    public byte[] hash(byte[] in) {
        return digest().digest(in);
    }

    public void hash(byte[] in, byte[] out, int outOff) {
        byte[] hash = digest().digest(in);
        System.arraycopy(hash, 0, out, outOff, hash.length);
    }

    public int size() {
        return size;
    }

    private MessageDigest digest() {
        return tryDigest(algorithm);
    }

    static MessageDigest tryDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(algorithm + " is not available", e);
        }
    }
}
