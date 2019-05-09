package com.urbanspork.cipher;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;

import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public abstract class AbstractStreamBlockCipher implements Cipher {

    protected int ivl;

    protected StreamBlockCipher cipher;

    private volatile boolean inited;

    @Override
    public byte[] encrypt(byte[] in, byte[] key) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (!inited) {
            byte[] iv = new byte[ivl];
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
        byte[] iv = new byte[ivl];
        if (!inited) {
            System.arraycopy(in, 0, iv, 0, iv.length);
            int length = in.length - iv.length;
            byte[] temp = new byte[length];
            System.arraycopy(in, ivl, temp, 0, length);
            ParametersWithIV parametersWithIV = new ParametersWithIV(new KeyParameter(key), iv);
            cipher.init(false, parametersWithIV);
            in = temp;
            inited = true;
        }
        byte[] out = new byte[in.length];
        cipher.processBytes(in, 0, in.length, out, 0);
        return out;
    }

}
