
package com.urbanspork.common.cipher.base;

import static io.netty.buffer.Unpooled.buffer;

import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import com.urbanspork.common.cipher.Cipher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class BaseStreamCipher implements Cipher {

    private final StreamCipher cipher;
    private final int ivSize;

    private volatile boolean inited;

    public BaseStreamCipher(StreamCipher cipher, int ivSize) {
        this.cipher = cipher;
        this.ivSize = ivSize;
    }

    @Override
    public byte[] encrypt(byte[] input, byte[] key) throws Exception {
        ByteBuf buf = buffer();
        if (!inited) {
            byte[] iv = randomBytes(ivSize);
            ParametersWithIV parametersWithIV = new ParametersWithIV(new KeyParameter(key), iv);
            cipher.init(true, parametersWithIV);
            buf.writeBytes(iv);
            inited = true;
        }
        byte[] output = new byte[input.length];
        cipher.processBytes(input, 0, input.length, output, 0);
        buf.writeBytes(output);
        output = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false);
        buf.release();
        return output;
    }

    @Override
    public byte[] decrypt(byte[] input, byte[] key) throws Exception {
        byte[] iv = new byte[ivSize];
        if (!inited) {
            System.arraycopy(input, 0, iv, 0, iv.length);
            int length = input.length - iv.length;
            byte[] temp = new byte[length];
            System.arraycopy(input, ivSize, temp, 0, length);
            ParametersWithIV parametersWithIV = new ParametersWithIV(new KeyParameter(key), iv);
            cipher.init(false, parametersWithIV);
            input = temp;
            inited = true;
        }
        byte[] output = new byte[input.length];
        cipher.processBytes(input, 0, input.length, output, 0);
        return output;
    }

}
