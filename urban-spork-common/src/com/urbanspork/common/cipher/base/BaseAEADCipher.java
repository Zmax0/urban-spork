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

import java.util.List;

/**
 * AEAD Cipher
 *
 * @author Zmax0
 * @see <a href=https://shadowsocks.org/guide/aead.html">https://shadowsocks.org/guide/aead.html</a>
 */
public class BaseAEADCipher implements Cipher {

    /*
     * [encrypted payload length][length tag][encrypted payload][payload tag]
     */

    private static final int NONCE_SIZE = 12;
    private static final int TAG_SIZE = 16;
    private static final int PAYLOAD_SIZE = 16 * 1024 - 1;
    private static final int INIT_PAYLOAD_LENGTH = Integer.MIN_VALUE;
    private static final byte[] info = new byte[]{115, 115, 45, 115, 117, 98, 107, 101, 121};

    private final int saltSize;
    private final int macSize;
    private final AEADCipher cipher;
    private final byte[] nonce = new byte[NONCE_SIZE];

    private KeyParameter subKey;
    int payloadLength = INIT_PAYLOAD_LENGTH;

    private boolean initialized;

    public BaseAEADCipher(AEADCipher cipher, int saltSize, int macSize) {
        this.saltSize = saltSize;
        this.macSize = macSize;
        this.cipher = cipher;
    }

    @Override
    public void encrypt(ByteBuf in, byte[] key, ByteBuf out) throws InvalidCipherTextException {
        if (!initialized) {
            byte[] salt = randomBytes(saltSize);
            out.writeBytes(salt);
            subKey = generateSubKey(key, salt);
            initialized = true;
        }
        while (in.isReadable()) {
            int len = Math.min(in.readableBytes(), PAYLOAD_SIZE);
            byte[] temp = new byte[2 + TAG_SIZE + len + TAG_SIZE];
            Unpooled.wrappedBuffer(temp).setShort(0, len);
            cipher.init(true, generateCipherParameters());
            cipher.doFinal(temp, cipher.processBytes(temp, 0, 2, temp, 0));
            out.writeBytes(temp, 0, 2 + TAG_SIZE);
            in.readBytes(temp, 2 + TAG_SIZE, len);
            cipher.init(true, generateCipherParameters());
            cipher.doFinal(temp, 2 + TAG_SIZE + cipher.processBytes(temp, 2 + TAG_SIZE, len, temp, 2 + TAG_SIZE));
            out.writeBytes(temp, 2 + TAG_SIZE, len + TAG_SIZE);
        }
    }

    @Override
    public void decrypt(ByteBuf in, byte[] key, List<Object> out) throws InvalidCipherTextException {
        if (!initialized && in.readableBytes() >= saltSize) {
            byte[] salt = new byte[saltSize];
            in.readBytes(salt, 0, saltSize);
            subKey = generateSubKey(key, salt);
            initialized = true;
        }
        while (initialized && in.readableBytes() >= (payloadLength == INIT_PAYLOAD_LENGTH ? 2 + TAG_SIZE : payloadLength + TAG_SIZE)) {
            if (payloadLength == INIT_PAYLOAD_LENGTH) {
                byte[] payloadLengthBytes = new byte[2 + TAG_SIZE];
                in.readBytes(payloadLengthBytes);
                cipher.init(false, generateCipherParameters());
                cipher.doFinal(payloadLengthBytes, cipher.processBytes(payloadLengthBytes, 0, 2 + TAG_SIZE, payloadLengthBytes, 0));
                payloadLength = Unpooled.wrappedBuffer(payloadLengthBytes).readShort();
            } else {
                byte[] payload = new byte[payloadLength + TAG_SIZE];
                in.readBytes(payload);
                cipher.init(false, generateCipherParameters());
                cipher.doFinal(payload, cipher.processBytes(payload, 0, payloadLength + TAG_SIZE, payload, 0));
                out.add(in.alloc().buffer(payloadLength).writeBytes(payload, 0, payloadLength));
                payloadLength = INIT_PAYLOAD_LENGTH;
            }
        }
    }

    private CipherParameters generateCipherParameters() {
        CipherParameters parameters = new AEADParameters(subKey, macSize, nonce);
        increaseNonce();
        return parameters;
    }

    private void increaseNonce() {
        ByteBuf buf = Unpooled.wrappedBuffer(nonce);
        short i = buf.getShortLE(0);
        i++;
        buf.setShortLE(0, i);
    }

    private KeyParameter generateSubKey(byte[] key, byte[] salt) {
        byte[] out = new byte[salt.length];
        HKDFBytesGenerator generator = new HKDFBytesGenerator(new SHA1Digest());
        generator.init(new HKDFParameters(key, salt, info));
        generator.generateBytes(out, 0, out.length);
        return new KeyParameter(out);
    }

}