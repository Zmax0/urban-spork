package com.urbanspork.common.codec.aead;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

import java.util.function.Supplier;

public enum CipherMethods implements Supplier<CipherMethod> {

    AES_128_GCM, AES_265_GCM, CHACHA20_POLY1305;

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
