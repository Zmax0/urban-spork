package com.urbanspork.common.codec.aead;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

import java.util.function.Supplier;

public enum CipherMethods implements Supplier<CipherMethod> {

    AES_GCM, CHACHA20_POLY1305;

    @Override
    public CipherMethod get() {
        if (CHACHA20_POLY1305 == this) {
            return new CipherMethod() {
                @Override
                public AEADCipher cipher() {
                    return new ChaCha20Poly1305();
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
        } else {
            return new CipherMethod() {
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
    }
}
