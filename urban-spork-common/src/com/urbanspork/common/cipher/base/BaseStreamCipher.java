package com.urbanspork.common.cipher.base;

import com.urbanspork.common.cipher.Cipher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import static io.netty.buffer.Unpooled.buffer;

public class BaseStreamCipher implements Cipher {

    private final StreamCipher cipher;
    private final int ivSize;

    private boolean initialized;
    private byte[] temp;

    public BaseStreamCipher(StreamCipher cipher, int ivSize) {
        this.cipher = cipher;
        this.ivSize = ivSize;
    }

    @Override
    public byte[] encrypt(byte[] in, byte[] key) {
        ByteBuf buf = buffer();
        if (!initialized) {
            byte[] iv = randomBytes(ivSize);
            ParametersWithIV parametersWithIV = new ParametersWithIV(new KeyParameter(key), iv);
            cipher.init(true, parametersWithIV);
            buf.writeBytes(iv);
            initialized = true;
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
        byte[] _in;
        if (temp != null) {
            _in = new byte[in.length + temp.length];
            System.arraycopy(temp, 0, _in, 0, temp.length);
            System.arraycopy(in, 0, _in, temp.length, in.length);
            temp = null;
        } else {
            _in = in;
        }
        if (!initialized) {
            if (_in.length < ivSize) {
                temp = _in;
                return empty;
            } else {
                byte[] iv = new byte[ivSize];
                System.arraycopy(_in, 0, iv, 0, iv.length);
                ParametersWithIV parametersWithIV = new ParametersWithIV(new KeyParameter(key), iv);
                cipher.init(false, parametersWithIV);
                initialized = true;
                byte[] out = new byte[_in.length - iv.length];
                cipher.processBytes(_in, iv.length, _in.length - iv.length, out, 0);
                return out;
            }
        } else {
            byte[] out = new byte[_in.length];
            cipher.processBytes(_in, 0, _in.length, out, 0);
            return out;
        }
    }

}
