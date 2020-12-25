
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

    private boolean inited;
    private byte[] temp;

    public BaseStreamCipher(StreamCipher cipher, int ivSize) {
        this.cipher = cipher;
        this.ivSize = ivSize;
    }

    @Override
    public byte[] encrypt(byte[] in, byte[] key) {
        ByteBuf buf = buffer();
        if (!inited) {
            byte[] iv = randomBytes(ivSize);
            ParametersWithIV parametersWithIV = new ParametersWithIV(new KeyParameter(key), iv);
            cipher.init(true, parametersWithIV);
            buf.writeBytes(iv);
            inited = true;
        }
        byte[] out = new byte[in.length];
        cipher.processBytes(in, 0, in.length, out, 0);
        buf.writeBytes(out);
        out = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false);
        buf.release();
        return out;
    }

    @Override
    public byte[] decrypt(byte[] in, byte[] key) {
        if (temp != null) {
            byte[] bytes = new byte[in.length + temp.length];
            System.arraycopy(temp, 0, bytes, 0, temp.length);
            System.arraycopy(in, 0, bytes, temp.length, in.length);
            in = bytes;
            temp = null;
        }
        byte[] _in = null;
        if (!inited) {
            if (in.length < ivSize) {
                temp = in;
                return empty;
            } else {
                byte[] iv = new byte[ivSize];
                System.arraycopy(in, 0, iv, 0, iv.length);
                int length = in.length - iv.length;
                _in = new byte[length];
                System.arraycopy(in, ivSize, _in, 0, length);
                ParametersWithIV parametersWithIV = new ParametersWithIV(new KeyParameter(key), iv);
                cipher.init(false, parametersWithIV);
                inited = true;
            }
        }
        byte[] out;
        if (_in == null) {
            out = new byte[in.length];
            cipher.processBytes(in, 0, in.length, out, 0);
        } else {
            out = new byte[_in.length];
            cipher.processBytes(_in, 0, _in.length, out, 0);
        }
        return out;
    }

}
