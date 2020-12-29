package com.urbanspork.common.cipher.base;

import com.urbanspork.common.cipher.Cipher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import static io.netty.buffer.Unpooled.buffer;

/**
 * AEAD Cipher
 *
 * @author Zmax0
 *
 * @see <a href=https://shadowsocks.org/en/wiki/AEAD-Ciphers.html">https://shadowsocks.org/en/wiki/AEAD-Ciphers.html</a>
 */
public class BaseAEADCipher implements Cipher {

    /*
     * [encrypted payload length][length tag][encrypted payload][payload tag]
     */

    private static final int nonceSize = 12;
    private static final int tagSize = 16;
    private static final int payloadSize = 16 * 1024 - 1;
    private static final byte[] info = new byte[] { 115, 115, 45, 115, 117, 98, 107, 101, 121 };

    private final int saltSize;
    private final int macSize;
    private final AEADCipher cipher;
    private final ByteBuf nonce = buffer(nonceSize);
    private final ByteBuf buffer = buffer();

    private int payloadLength = -1;
    private KeyParameter subkey;

    private boolean initialized;

    public BaseAEADCipher(AEADCipher cipher, int saltSize, int macSize) {
        this.saltSize = saltSize;
        this.macSize = macSize;
        this.cipher = cipher;
    }

    @Override
    public byte[] encrypt(byte[] in, byte[] key) throws Exception {
        ByteBuf buf = buffer();
        if (!initialized) {
            byte[] salt = randomBytes(saltSize);
            buf.writeBytes(salt);
            subkey = generateSubkey(key, salt);
            initialized = true;
        }
        ByteBuf _in = buffer(in.length);
        _in.writeBytes(in);
        while (_in.isReadable()) {
            int payloadLength = Math.min(_in.readableBytes(), payloadSize);
            byte[] temp = new byte[2 + tagSize + payloadLength + tagSize];
            // Payload length is a 2-byte big-endian unsigned integer
            ByteBuf encryptBuff = buffer(2);
            encryptBuff.writeShort(payloadLength);
            encryptBuff.readBytes(temp, 0, 2);
            encryptBuff.release();
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
        ByteBuf buf = buffer();
        ByteBuf _in = buffer(in.length + buffer.readableBytes());
        if (buffer.isReadable()) {
            _in.writeBytes(buffer);
        }
        _in.writeBytes(in);
        if (!initialized) {
            if (_in.readableBytes() < saltSize) {
                buffer.writeBytes(_in);
                return empty;
            } else {
                byte[] salt = new byte[saltSize];
                _in.readBytes(salt, 0, saltSize);
                subkey = generateSubkey(key, salt);
                initialized = true;
            }
        }
        while (_in.isReadable()) {
            if (payloadLength == -1) {
                if (_in.readableBytes() < 2 + tagSize) {
                    buffer.writeBytes(_in);
                    break;
                }
                byte[] payloadLengthBytes = new byte[2 + tagSize];
                _in.readBytes(payloadLengthBytes, 0, 2 + tagSize);
                cipher.init(false, generateCipherParameters());
                try {
                    cipher.doFinal(payloadLengthBytes, cipher.processBytes(payloadLengthBytes, 0, 2 + tagSize, payloadLengthBytes, 0));
                } catch (Exception e) {
                    buffer.writeBytes(_in);
                    return empty;
                }
                ByteBuf _payloadLength = buffer(payloadLengthBytes.length);
                _payloadLength.writeBytes(payloadLengthBytes);
                payloadLength = _payloadLength.getShort(0);
                _payloadLength.release();
            }
            if (_in.readableBytes() < payloadLength + tagSize) {
                buffer.writeBytes(_in);
                break;
            }
            byte[] payload = new byte[payloadLength + tagSize];
            _in.readBytes(payload, 0, payloadLength + tagSize);
            cipher.init(false, generateCipherParameters());
            cipher.doFinal(payload, cipher.processBytes(payload, 0, payloadLength + tagSize, payload, 0));
            buf.writeBytes(payload, 0, payloadLength);
            payloadLength = -1;
        }
        byte[] out = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false);
        buf.release();
        _in.release();
        return out;
    }

    private CipherParameters generateCipherParameters() {
        CipherParameters parameters = new AEADParameters(subkey, macSize, nonce.array());
        increaseNonce(nonce);
        return parameters;
    }

    private void increaseNonce(ByteBuf nonce) {
        short i = nonce.getShortLE(0);
        i++;
        nonce.setShortLE(0, i);
    }

    private KeyParameter generateSubkey(byte[] key, byte[] salt) {
        byte[] out = new byte[salt.length];
        HKDFBytesGenerator generator = new HKDFBytesGenerator(new SHA1Digest());
        generator.init(new HKDFParameters(key, salt, info));
        generator.generateBytes(out, 0, out.length);
        return new KeyParameter(out);
    }

}
