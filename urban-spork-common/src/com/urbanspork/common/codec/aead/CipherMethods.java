package com.urbanspork.common.codec.aead;

import com.urbanspork.jni.xchacha20poly1305.Cipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

import java.util.Arrays;
import java.util.function.Supplier;

public enum CipherMethods implements Supplier<CipherMethod> {

    AES_128_GCM, AES_265_GCM, CHACHA20_POLY1305, XCHACHA20_POLY1305;

    @Override
    public CipherMethod get() {
        switch (this) {
            case AES_128_GCM -> {
                return new CipherMethod() {
                    @Override
                    public int keySize() {
                        return 16;
                    }

                    @Override
                    public AEADCipher cipher() {
                        return GCMBlockCipher.newInstance(AESEngine.newInstance());
                    }

                    @Override
                    public int macSize() {
                        return 128;
                    }

                    @Override
                    public int nonceSize() {
                        return 12;
                    }
                };
            }
            case AES_265_GCM -> {
                return new CipherMethod() {
                    @Override
                    public int keySize() {
                        return 32;
                    }

                    @Override
                    public AEADCipher cipher() {
                        return GCMBlockCipher.newInstance(AESEngine.newInstance());
                    }

                    @Override
                    public int macSize() {
                        return 128;
                    }

                    @Override
                    public int nonceSize() {
                        return 12;
                    }
                };
            }
            case XCHACHA20_POLY1305 -> {
                return new CipherMethod() {
                    @Override
                    public int keySize() {
                        return 32;
                    }

                    @Override
                    public AEADCipher cipher() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public int macSize() {
                        return 128;
                    }

                    @Override
                    public int nonceSize() {
                        return 24;
                    }

                    @Override
                    public byte[] encrypt(byte[] secretKey, byte[] nonce, byte[] in) {
                        return encrypt(secretKey, nonce, new byte[]{}, in);
                    }

                    @Override
                    public byte[] encrypt(byte[] secretKey, byte[] nonce, byte[] associatedText, byte[] in) {
                        Cipher cipher = Cipher.init(secretKey);
                        byte[] out = Arrays.copyOf(in, in.length + 16);
                        cipher.encrypt(nonce, associatedText, out);
                        return out;
                    }

                    @Override
                    public byte[] decrypt(byte[] secretKey, byte[] nonce, byte[] in) {
                        return decrypt(secretKey, nonce, new byte[]{}, in);
                    }

                    @Override
                    public byte[] decrypt(byte[] secretKey, byte[] nonce, byte[] associatedText, byte[] in) {
                        Cipher cipher = Cipher.init(secretKey);
                        cipher.decrypt(nonce, associatedText, in);
                        return Arrays.copyOfRange(in, 0, in.length - 16);
                    }
                };
            }

            default -> {
                return new CipherMethod() {
                    @Override
                    public AEADCipher cipher() {
                        return new ChaCha20Poly1305();
                    }

                    @Override
                    public int keySize() {
                        return 32;
                    }

                    @Override
                    public int macSize() {
                        return 128;
                    }

                    @Override
                    public int nonceSize() {
                        return 12;
                    }
                };
            }
        }
    }
}
