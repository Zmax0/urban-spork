package com.urbanspork.common.protocol.shadowsocks.aead;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.EmptyPaddingLengthGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import com.urbanspork.common.codec.aead.Authenticator;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.PayloadDecoder;
import com.urbanspork.common.codec.aead.PayloadEncoder;
import com.urbanspork.common.codec.chunk.AEADChunkSizeParser;
import com.urbanspork.common.codec.chunk.EmptyChunkSizeParser;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

public interface AEAD {

    /**
     * OpenSSL EVP_BytesToKey
     */
    static byte[] opensslBytesToKey(byte[] password, int size) {
        byte[] key = new byte[size];
        int offset = 0;
        byte[] last = null;
        MD5Digest digest = new MD5Digest();
        while (offset < size) {
            if (last != null) {
                digest.update(last, 0, last.length);
            }
            digest.update(password, 0, password.length);
            byte[] out = new byte[digest.getDigestSize()];
            digest.doFinal(out, 0);
            int add = Math.min(size - offset, out.length);
            System.arraycopy(out, 0, key, offset, add);
            offset += add;
            last = out;
        }
        return key;
    }

    interface TCP {
        static PayloadEncoder newPayloadEncoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(hkdfsha1(key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadEncoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE, 0x3fff);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(hkdfsha1(key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadDecoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE);
        }
    }

    interface UDP {
        static PayloadEncoder newPayloadEncoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(hkdfsha1(key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            return new PayloadEncoder(auth, EmptyChunkSizeParser.INSTANCE, EmptyPaddingLengthGenerator.INSTANCE, 0x3fff);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(hkdfsha1(key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            return new PayloadDecoder(auth, EmptyChunkSizeParser.INSTANCE, EmptyPaddingLengthGenerator.INSTANCE);
        }
    }

    private static byte[] hkdfsha1(byte[] key, byte[] salt) {
        byte[] out = new byte[salt.length];
        HKDFBytesGenerator generator = new HKDFBytesGenerator(new SHA1Digest());
        generator.init(new HKDFParameters(key, salt, "ss-subkey".getBytes()));
        generator.generateBytes(out, 0, out.length);
        return out;
    }
}
