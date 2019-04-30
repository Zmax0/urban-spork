package com.urbanspork.cipher.impl;

import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import com.urbanspork.cipher.AbstractCipher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class StreamCiphers extends AbstractCipher {

    private final StreamCipher cipher;
    private final int ivSize;

    public StreamCiphers(StreamCipher cipher, int ivSize) {
        this.cipher = cipher;
        this.ivSize = ivSize;
    }

    @Override
    public byte[] encrypt(byte[] in, byte[] key) throws Exception {
        ByteBuf result = Unpooled.buffer();
        if (!inited) {
            byte[] iv = randomBytes(ivSize);
            ParametersWithIV parametersWithIV = new ParametersWithIV(new KeyParameter(key), iv);
            cipher.init(true, parametersWithIV);
            result.writeBytes(iv);
            inited = true;
        }
        byte[] out = new byte[in.length];
        cipher.processBytes(in, 0, in.length, out, 0);
        result.writeBytes(out);
        return ByteBufUtil.getBytes(result);
    }

    @Override
    public byte[] decrypt(byte[] in, byte[] key) throws Exception {
        byte[] iv = new byte[ivSize];
        if (!inited) {
            System.arraycopy(in, 0, iv, 0, iv.length);
            int length = in.length - iv.length;
            byte[] temp = new byte[length];
            System.arraycopy(in, ivSize, temp, 0, length);
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
