package com.urbanspork.common.cipher.base;

import com.urbanspork.common.cipher.Cipher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
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
 * @see <a href=https://shadowsocks.org/en/wiki/AEAD-Ciphers.html">https://shadowsocks.org/en/wiki/AEAD-Ciphers.html</a>
 */
public class BaseAEADCipher implements Cipher {

    /*
     * [encrypted payload length][length tag][encrypted payload][payload tag]
     */

    private static final int NONCE_SIZE = 12;
    private static final int TAG_SIZE = 16;
    private static final int PAYLOAD_SIZE = 16 * 1024 - 1;
    private static final byte[] info = new byte[]{115, 115, 45, 115, 117, 98, 107, 101, 121};

    private final int saltSize;
    private final int macSize;
    private final AEADCipher cipher;
    private final ByteBuf nonce = buffer(NONCE_SIZE);
    private final ByteBuf buffer = buffer();

    private int payloadLength = -1;
    private KeyParameter subKey;

    private boolean initialized;

    public BaseAEADCipher(AEADCipher cipher, int saltSize, int macSize) {
        this.saltSize = saltSize;
        this.macSize = macSize;
        this.cipher = cipher;
    }

    @Override
    public ByteBuf encrypt(ByteBuf in, byte[] key) throws InvalidCipherTextException {
        ByteBuf buf = buffer();
        if (!initialized) {
            byte[] salt = randomBytes(saltSize);
            buf.writeBytes(salt);
            subKey = generateSubKey(key, salt);
            initialized = true;
        }
        ByteBuf in0 = buffer(in.readableBytes());
        in0.writeBytes(in);
        while (in0.isReadable()) {
            int len = Math.min(in0.readableBytes(), PAYLOAD_SIZE);
            byte[] temp = new byte[2 + TAG_SIZE + len + TAG_SIZE];
            // Payload length is a 2-byte big-endian unsigned integer
            ByteBuf encryptBuff = buffer(2);
            encryptBuff.writeShort(len);
            encryptBuff.readBytes(temp, 0, 2);
            encryptBuff.release();
            cipher.init(true, generateCipherParameters());
            cipher.doFinal(temp, cipher.processBytes(temp, 0, 2, temp, 0));
            buf.writeBytes(temp, 0, 2 + TAG_SIZE);
            in0.readBytes(temp, 2 + TAG_SIZE, len);
            cipher.init(true, generateCipherParameters());
            cipher.doFinal(temp, 2 + TAG_SIZE + cipher.processBytes(temp, 2 + TAG_SIZE, len, temp, 2 + TAG_SIZE));
            buf.writeBytes(temp, 2 + TAG_SIZE, len + TAG_SIZE);
        }
        return buf;
    }

    @Override
    public ByteBuf decrypt(ByteBuf in, byte[] key) throws InvalidCipherTextException {
        buffer.writeBytes(in);
        if (!initialized) {
            if (buffer.readableBytes() < saltSize) {
                return Unpooled.EMPTY_BUFFER;
            } else {
                byte[] salt = new byte[saltSize];
                buffer.readBytes(salt, 0, saltSize);
                subKey = generateSubKey(key, salt);
                initialized = true;
            }
        }
        ByteBuf buf = buffer();
        while (buffer.isReadable()) {
            if (payloadLength == -1) {
                byte[] payloadLengthBytes = new byte[2 + TAG_SIZE];
                buffer.readBytes(payloadLengthBytes, 0, 2 + TAG_SIZE);
                cipher.init(false, generateCipherParameters());
                cipher.doFinal(payloadLengthBytes, cipher.processBytes(payloadLengthBytes, 0, 2 + TAG_SIZE, payloadLengthBytes, 0));
                ByteBuf len0 = buffer(payloadLengthBytes.length);
                len0.writeBytes(payloadLengthBytes);
                payloadLength = len0.getShort(0);
                len0.release();
            }
            if (buffer.readableBytes() < payloadLength + TAG_SIZE) {
                break;
            }
            byte[] payload = new byte[payloadLength + TAG_SIZE];
            buffer.readBytes(payload, 0, payloadLength + TAG_SIZE);
            cipher.init(false, generateCipherParameters());
            cipher.doFinal(payload, cipher.processBytes(payload, 0, payloadLength + TAG_SIZE, payload, 0));
            buf.writeBytes(payload, 0, payloadLength);
            payloadLength = -1;
        }
        return buf;
    }

    @Override
    public void releaseBuffer() {
        buffer.release();
        nonce.release();
    }

    private CipherParameters generateCipherParameters() {
        CipherParameters parameters = new AEADParameters(subKey, macSize, nonce.array());
        increaseNonce(nonce);
        return parameters;
    }

    private void increaseNonce(ByteBuf nonce) {
        short i = nonce.getShortLE(0);
        i++;
        nonce.setShortLE(0, i);
    }

    private KeyParameter generateSubKey(byte[] key, byte[] salt) {
        byte[] out = new byte[salt.length];
        HKDFBytesGenerator generator = new HKDFBytesGenerator(new SHA1Digest());
        generator.init(new HKDFParameters(key, salt, info));
        generator.generateBytes(out, 0, out.length);
        return new KeyParameter(out);
    }

}
