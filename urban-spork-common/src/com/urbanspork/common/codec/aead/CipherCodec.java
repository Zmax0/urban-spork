package com.urbanspork.common.codec.aead;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

public interface CipherCodec {

    AEADCipher cipher();

    int macSize();

    int nonceSize();

    default int tagSize() {
        return 16;
    }

    default byte[] encrypt(byte[] secretKey, byte[] nonce, byte[] in) throws InvalidCipherTextException {
        return encrypt(secretKey, nonce, null, in);
    }

    default byte[] encrypt(byte[] secretKey, byte[] nonce, byte[] associatedText, byte[] in) throws InvalidCipherTextException {
        AEADCipher cipher = cipher();
        cipher.init(true, new AEADParameters(new KeyParameter(secretKey), macSize(), nonce, associatedText));
        byte[] out = new byte[cipher.getOutputSize(in.length)];
        cipher.doFinal(out, cipher.processBytes(in, 0, in.length, out, 0));
        return out;
    }

    default byte[] decrypt(byte[] secretKey, byte[] nonce, byte[] in) throws InvalidCipherTextException {
        return decrypt(secretKey, nonce, null, in);
    }

    default byte[] decrypt(byte[] secretKey, byte[] nonce, byte[] associatedText, byte[] in) throws InvalidCipherTextException {
        AEADCipher cipher = cipher();
        cipher.init(false, new AEADParameters(new KeyParameter(secretKey), macSize(), nonce, associatedText));
        byte[] out = new byte[cipher.getOutputSize(in.length)];
        cipher.doFinal(out, cipher.processBytes(in, 0, in.length, out, 0));
        return out;
    }
}
