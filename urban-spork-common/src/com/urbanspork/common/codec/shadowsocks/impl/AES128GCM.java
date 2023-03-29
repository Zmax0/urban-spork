package com.urbanspork.common.codec.shadowsocks.impl;

import com.urbanspork.common.codec.aead.AEADCipherCodec;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

public class AES128GCM extends ShadowsocksCipherCodec {

    public AES128GCM(byte[] password) {
        super(password, 16);
    }

    @Override
    protected AEADCipherCodec codec() {
        return new AEADCipherCodec() {
            @Override
            public AEADCipher cipher() {
                return new GCMBlockCipher(new AESEngine());
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