package com.urbanspork.common.codec.shadowsocks.impl;

import com.urbanspork.common.codec.aead.AEADCipherCodec;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;

public class ChaCha20IETFPoly1305 extends ShadowsocksCipherCodec {

    public ChaCha20IETFPoly1305(byte[] password) {
        super(password, 32);
    }

    @Override
    protected AEADCipherCodec codec() {
        return new AEADCipherCodec() {
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
    }

}
