package com.urbanspork.common.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Security;
import java.util.function.Supplier;

public enum AES {

    DEFAULT(() -> getCipher("AES")),
    ECB_PKCS7Padding(() -> getCipher("AES/ECB/PKCS7Padding")),
    ECB_NoPadding(() -> getCipher("AES/ECB/NoPadding"));

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Supplier<Cipher> provider;

    AES(Supplier<Cipher> provider) {
        this.provider = provider;
    }

    public byte[] encrypt(byte[] key, byte[] in) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Cipher cipher = provider.get();
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(in);
    }

    public byte[] decrypt(byte[] key, byte[] in) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = provider.get();
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(in);
    }

    private static Cipher getCipher(String transformation) {
        try {
            return Cipher.getInstance(transformation, BouncyCastleProvider.PROVIDER_NAME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
