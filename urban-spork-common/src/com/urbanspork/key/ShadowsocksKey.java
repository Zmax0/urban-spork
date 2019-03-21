package com.urbanspork.key;

import static java.lang.System.arraycopy;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.crypto.SecretKey;

public class ShadowsocksKey implements SecretKey {

    private static final long serialVersionUID = 20181226;

    private static final int DEFAULT_KEY_LENGTH = 32;

    private static MessageDigest MD5 = null;

    static {
        try {
            MD5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private String password;

    private transient byte[] key;

    public ShadowsocksKey(String password) {
        super();
        this.password = password;
        this.key = generateKey(password);
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
        return key;
    }

    public String getPassword() {
        return password;
    }

    private byte[] generateKey(String _password) {
        Optional<String> password = Optional.of(_password);
        byte[] key = new byte[DEFAULT_KEY_LENGTH];
        byte[] passwordBytes = null;
        byte[] passwordDigest = null;
        byte[] container = null;
        int index = 0;
        try {
            passwordBytes = password.get().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
        while (index < DEFAULT_KEY_LENGTH) {
            if (index == 0) {
                passwordDigest = MD5.digest(passwordBytes);
                container = new byte[passwordBytes.length + passwordDigest.length];
            } else {
                arraycopy(passwordDigest, 0, container, 0, passwordDigest.length);
                arraycopy(passwordBytes, 0, container, passwordDigest.length, passwordBytes.length);
                passwordDigest = MD5.digest(container);
            }
            arraycopy(passwordDigest, 0, key, index, passwordDigest.length);
            index += passwordDigest.length;
        }
        return key;
    }

}
