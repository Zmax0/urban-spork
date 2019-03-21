package com.urbanspork.cipher;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;

import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public interface SymmetricEncryptUtils {

    byte[] encrypt(byte[] in, byte[] key) throws Exception;

    String encrypt(String in, String key) throws Exception;

    byte[] decrypt(byte[] in, byte[] key) throws Exception;

    String decrypt(String in, String key) throws Exception;

    static class AES implements SymmetricEncryptUtils {

        private static final int IV_LENGTH = 16;

        private StreamBlockCipher cipher;

        private volatile boolean inited;

        private AES() {

        }

        public static AES AES_256_CFB() {
            AES aes = new AES();
            aes.cipher = new CFBBlockCipher(new AESEngine(), 128);
            return aes;
        }

        @Override
        public byte[] encrypt(byte[] in, byte[] key) throws Exception {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (!inited) {
                byte[] iv = new byte[IV_LENGTH];
                new SecureRandom().nextBytes(iv);
                ParametersWithIV parametersWithIV = new ParametersWithIV(new KeyParameter(key), iv);
                cipher.init(true, parametersWithIV);
                stream.write(iv);
                inited = true;
            }
            byte[] out = new byte[in.length];
            cipher.processBytes(in, 0, in.length, out, 0);
            stream.write(out);
            return stream.toByteArray();
        }

        @Override
        public byte[] decrypt(byte[] in, byte[] key) throws Exception {
            byte[] iv = new byte[IV_LENGTH];
            if (!inited) {
                System.arraycopy(in, 0, iv, 0, iv.length);
                int length = in.length - iv.length;
                byte[] temp = new byte[length];
                System.arraycopy(in, IV_LENGTH, temp, 0, length);
                ParametersWithIV parametersWithIV = new ParametersWithIV(new KeyParameter(key), iv);
                cipher.init(false, parametersWithIV);
                in = temp;
                inited = true;
            }
            byte[] out = new byte[in.length];
            cipher.processBytes(in, 0, in.length, out, 0);
            return out;
        }

        @Override
        public String encrypt(String in, String key) throws Exception {
            return Base64.getEncoder().encodeToString(encrypt(in.getBytes(), key.getBytes()));
        }

        @Override
        public String decrypt(String in, String key) throws Exception {
            return new String(decrypt(Base64.getDecoder().decode(in), key.getBytes()));
        }

    }

}
