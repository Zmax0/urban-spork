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
import com.urbanspork.common.crypto.Digests;
import com.urbanspork.common.crypto.HKDF;
import com.urbanspork.common.util.Bytes;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface AEAD {

    /**
     * OpenSSL EVP_BytesToKey
     */
    static byte[] opensslBytesToKey(byte[] password, int size) {
        byte[] key = new byte[size];
        int offset = 0;
        byte[] last = null;
        while (offset < size) {
            byte[] out = Digests.md5.hash(last == null ? password : Bytes.concat(last, password));
            int add = Math.min(size - offset, out.length);
            System.arraycopy(out, 0, key, offset, add);
            offset += add;
            last = out;
        }
        return key;
    }

    interface TCP {
        static PayloadEncoder newPayloadEncoder(CipherMethod cipherMethod, byte[] key, byte[] salt) throws NoSuchAlgorithmException, InvalidKeyException {
            Authenticator auth = new Authenticator(hkdfsha1(key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadEncoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE, 0x3fff);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, byte[] key, byte[] salt) throws NoSuchAlgorithmException, InvalidKeyException {
            Authenticator auth = new Authenticator(hkdfsha1(key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadDecoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE);
        }
    }

    interface UDP {
        static PayloadEncoder newPayloadEncoder(CipherMethod cipherMethod, byte[] key, byte[] salt) throws NoSuchAlgorithmException, InvalidKeyException {
            Authenticator auth = new Authenticator(hkdfsha1(key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            return new PayloadEncoder(auth, EmptyChunkSizeParser.INSTANCE, EmptyPaddingLengthGenerator.INSTANCE, 0x3fff);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, byte[] key, byte[] salt) throws NoSuchAlgorithmException, InvalidKeyException {
            Authenticator auth = new Authenticator(hkdfsha1(key, salt), cipherMethod, NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            return new PayloadDecoder(auth, EmptyChunkSizeParser.INSTANCE, EmptyPaddingLengthGenerator.INSTANCE);
        }
    }

    private static byte[] hkdfsha1(byte[] key, byte[] salt) throws NoSuchAlgorithmException, InvalidKeyException {
        return HKDF.sha1(key, salt, "ss-subkey".getBytes(), salt.length);
    }
}
