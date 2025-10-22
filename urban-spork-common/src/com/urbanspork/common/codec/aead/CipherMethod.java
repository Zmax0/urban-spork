package com.urbanspork.common.codec.aead;

import com.urbanspork.jni.AeadCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.Arrays;

public enum CipherMethod {
    AES_128_GCM(16, 12, 16),
    AES_265_GCM(32, 12, 16),
    CHACHA8_POLY1305(32, 12, 16),
    CHACHA20_POLY1305(32, 12, 16),
    XCHACHA8_POLY1305(32, 24, 16),
    XCHACHA20_POLY1305(32, 24, 16),
    ;

    private final int keySize;
    private final int nonceSize;
    private final int tagSize;

    CipherMethod(int keySize, int nonceSize, int tagSize) {
        this.keySize = keySize;
        this.nonceSize = nonceSize;
        this.tagSize = tagSize;
    }

    public int keySize() {
        return keySize;
    }

    public int nonceSize() {
        return nonceSize;
    }

    public int tagSize() {
        return tagSize;
    }

    public CipherInstance init(byte[] key) {
        return switch (this) {
            case AES_128_GCM, AES_265_GCM -> new BouncyCastleCipherInstance(GCMBlockCipher.newInstance(AESEngine.newInstance()), key);
            case CHACHA8_POLY1305 -> new JniCipherInstance(com.urbanspork.jni.chacha8poly1305.Cipher.newInstance(key), tagSize);
            case XCHACHA8_POLY1305 -> new JniCipherInstance(com.urbanspork.jni.xchacha8poly1305.Cipher.newInstance(key), tagSize);
            case XCHACHA20_POLY1305 -> new JniCipherInstance(com.urbanspork.jni.xchacha20poly1305.Cipher.newInstance(key), tagSize);
            default -> new BouncyCastleCipherInstance(new ChaCha20Poly1305(), key);
        };
    }

    private record BouncyCastleCipherInstance(AEADCipher cipher, KeyParameter key) implements CipherInstance {
        BouncyCastleCipherInstance(AEADCipher cipher, byte[] key) {
            this(cipher, new KeyParameter(key));
        }

        @Override
        public byte[] encrypt(byte[] nonce, byte[] aad, byte[] in) throws InvalidCipherTextException {
            cipher.init(true, new AEADParameters(key, 128, nonce, aad));
            byte[] out = new byte[cipher.getOutputSize(in.length)];
            cipher.doFinal(out, cipher.processBytes(in, 0, in.length, out, 0));
            return out;
        }

        @Override
        public byte[] decrypt(byte[] nonce, byte[] aad, byte[] in) throws InvalidCipherTextException {
            cipher.init(false, new AEADParameters(key, 128, nonce, aad));
            byte[] out = new byte[cipher.getOutputSize(in.length)];
            cipher.doFinal(out, cipher.processBytes(in, 0, in.length, out, 0));
            return out;
        }
    }

    private record JniCipherInstance(AeadCipher cipher, int tagSize) implements CipherInstance {
        @Override
        public byte[] encrypt(byte[] nonce, byte[] aad, byte[] in) {
            byte[] out = Arrays.copyOf(in, in.length + tagSize);
            cipher.encrypt(nonce, aad, out);
            return out;
        }

        @Override
        public byte[] decrypt(byte[] nonce, byte[] aad, byte[] in) {
            cipher.decrypt(nonce, aad, in);
            return Arrays.copyOfRange(in, 0, in.length - tagSize);
        }
    }
}
