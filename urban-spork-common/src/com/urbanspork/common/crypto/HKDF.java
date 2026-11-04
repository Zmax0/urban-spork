package com.urbanspork.common.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public final class HKDF {

    private HKDF() {}

    public static byte[] sha1(byte[] ikm, byte[] salt, byte[] info, int length) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(salt, "HmacSHA1"));
        byte[] pseudoRandomKey = mac.doFinal(ikm);
        byte[] okm = new byte[length];
        byte[] previous = new byte[0];
        for (int block = 1, offset = 0; offset < length; block++) {
            mac.init(new SecretKeySpec(pseudoRandomKey, "HmacSHA1"));
            mac.update(previous);
            mac.update(info);
            mac.update((byte) block);
            previous = mac.doFinal();
            int n = Math.min(previous.length, length - offset);
            System.arraycopy(previous, 0, okm, offset, n);
            offset += n;
        }
        return okm;
    }

}
