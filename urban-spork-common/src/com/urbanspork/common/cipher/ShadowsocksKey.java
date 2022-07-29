package com.urbanspork.common.cipher;

import javax.crypto.SecretKey;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.lang.System.arraycopy;

public class ShadowsocksKey implements SecretKey {

    @Serial
    private static final long serialVersionUID = 20181226;

    private static final MessageDigest MD5;

    static {
        try {
            MD5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private final String password;

    private final int length;

    private final transient byte[] key;

    public ShadowsocksKey(String password, int length) {
        this.password = password;
        this.length = length;
        this.key = getEncode();
    }

    @Override
    public String toString() {
        return password;
    }

    @Override
    public String getAlgorithm() {
        return null;
    }

    @Override
    public String getFormat() {
        return null;
    }

    @Override
    public byte[] getEncoded() {
        return this.key;
    }

    private byte[] getEncode() {
        byte[] passwordDigest = null;
        byte[] container = null;
        int index = 0;
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] encoded = new byte[length];
        while (index < length) {
            if (index == 0) {
                passwordDigest = MD5.digest(passwordBytes);
                container = new byte[passwordBytes.length + passwordDigest.length];
            } else {
                arraycopy(passwordDigest, 0, container, 0, passwordDigest.length);
                arraycopy(passwordBytes, 0, container, passwordDigest.length, passwordBytes.length);
                passwordDigest = MD5.digest(container);
            }
            arraycopy(passwordDigest, 0, encoded, index, Math.min(length - index, passwordDigest.length));
            index += passwordDigest.length;
        }
        return encoded;
    }
}
