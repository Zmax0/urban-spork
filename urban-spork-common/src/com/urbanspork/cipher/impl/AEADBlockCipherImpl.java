package com.urbanspork.cipher.impl;

import static io.netty.buffer.Unpooled.directBuffer;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Arrays;

import com.urbanspork.cipher.Cipher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

/**
 * AEAD Cipher
 * 
 * @author Zmax0
 * 
 * @see <a href=https://shadowsocks.org/en/spec/AEAD-Ciphers.html">https://shadowsocks.org/en/spec/AEAD-Ciphers.html</a>
 */
public class AEADBlockCipherImpl implements Cipher {

    private static final int nonceSize = 12;
    private static final int tagSize = 16;
    private static final int payloadSize = 0x3FFF;
    private static final byte[] info = new byte[] { 115, 115, 45, 115, 117, 98, 107, 101, 121 };
    private final byte[] nonce = new byte[nonceSize];

    private final int saltSize;
    private final int macSize;
    private final AEADBlockCipher cipher;

    private volatile boolean inited;
    private byte[] subkey;
    private byte[] temp;
    private int payloadLength;

    public AEADBlockCipherImpl(AEADBlockCipher cipher, int saltSize, int macSize) {
        this.saltSize = saltSize;
        this.macSize = macSize;
        this.cipher = cipher;
    }

    @Override
    public byte[] encrypt(byte[] in, byte[] key) throws Exception {
        ByteBuf buf = directBuffer();
        if (!inited) {
            inited = true;
            byte[] salt = randomBytes(saltSize);
            buf.writeBytes(salt);
            subkey = generateSubkey(key, salt);
            temp = new byte[2 + tagSize + payloadSize + tagSize];
        }
        ByteBuf _in = directBuffer(in.length);
        _in.writeBytes(in);
        while (_in.isReadable()) {
            int payloadLength = Math.min(_in.readableBytes(), payloadSize);
            ByteBuf encryptBuff = directBuffer(2);
            encryptBuff.writeShort(payloadLength);
            encryptBuff.readBytes(temp, 0, 2);
            cipher.init(true, generateCipherParameters());
            cipher.doFinal(temp, cipher.processBytes(temp, 0, 2, temp, 0));
            buf.writeBytes(temp, 0, 2 + tagSize);
            _in.readBytes(temp, 2 + tagSize, payloadLength);
            cipher.init(true, generateCipherParameters());
            cipher.doFinal(temp, 2 + tagSize + cipher.processBytes(temp, 2 + tagSize, payloadLength, temp, 2 + tagSize));
            buf.writeBytes(temp, 2 + tagSize, payloadLength + tagSize);
        }
        byte[] out = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false);
        buf.release();
        return out;
    }

    @Override
    public byte[] decrypt(byte[] in, byte[] key) throws Exception {
        ByteBuf buf = directBuffer();
        ByteBuf _in = directBuffer();
        if (temp != null) {
            _in.writeBytes(temp);
        }
        _in.writeBytes(in);
        if (!inited) {
            inited = true;
            byte[] salt = new byte[saltSize];
            _in.readBytes(salt, 0, salt.length);
            subkey = generateSubkey(key, salt);
        }
        while (_in.isReadable()) {
            if (_in.readableBytes() < 2 + tagSize) {
                temp = new byte[_in.readableBytes()];
                _in.readBytes(temp);
                break;
            }
            byte[] payloadLengthBytes = new byte[2 + tagSize];
            if (payloadLength == 0) {
                _in.readBytes(payloadLengthBytes, 0, 2 + tagSize);
                cipher.init(false, generateCipherParameters());
                cipher.doFinal(payloadLengthBytes, cipher.processBytes(payloadLengthBytes, 0, 2 + tagSize, payloadLengthBytes, 0));
                ByteBuf _payloadLength = directBuffer(payloadLengthBytes.length);
                _payloadLength.writeBytes(payloadLengthBytes);
                payloadLength = _payloadLength.getShort(0);
            }
            if (_in.readableBytes() < payloadLength + tagSize) {
                temp = new byte[_in.readableBytes()];
                _in.readBytes(temp);
                break;
            }
            byte[] payloadBytes = new byte[payloadLength + tagSize];
            _in.readBytes(payloadBytes, 0, payloadLength + tagSize);
            cipher.init(false, generateCipherParameters());
            cipher.doFinal(payloadBytes, cipher.processBytes(payloadBytes, 0, payloadLength + tagSize, payloadBytes, 0));
            buf.writeBytes(payloadBytes, 0, payloadLength);
            payloadLength = 0;
            temp = null;
        }
        byte[] out = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false);
        buf.release();
        return out;
    }

    private CipherParameters generateCipherParameters() {
        CipherParameters parameters = new AEADParameters(new KeyParameter(subkey), macSize, Arrays.copyOf(nonce, nonceSize));
        nonce[0]++;
        return parameters;
    }

    private byte[] generateSubkey(byte[] key, byte[] salt) {
        byte[] out = new byte[salt.length];
        HKDFBytesGenerator generator = new HKDFBytesGenerator(new SHA1Digest());
        generator.init(new HKDFParameters(key, salt, info));
        generator.generateBytes(out, 0, out.length);
        return out;
    }

}
