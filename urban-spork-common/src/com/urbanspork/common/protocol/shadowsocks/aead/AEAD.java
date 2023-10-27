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
import com.urbanspork.common.crypto.GeneralDigests;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import static java.lang.System.arraycopy;

public interface AEAD {

    static byte[] generateKey(byte[] password, int size) {
        byte[] encoded = new byte[size];
        byte[] passwordDigest = GeneralDigests.md5.get(password);
        byte[] container = new byte[password.length + passwordDigest.length];
        arraycopy(passwordDigest, 0, encoded, 0, Math.min(size, passwordDigest.length));
        int index = passwordDigest.length;
        while (index < size) {
            arraycopy(passwordDigest, 0, container, 0, passwordDigest.length);
            arraycopy(password, 0, container, passwordDigest.length, password.length);
            passwordDigest = GeneralDigests.md5.get(container);
            arraycopy(passwordDigest, 0, encoded, index, Math.min(size - index, passwordDigest.length));
            index += passwordDigest.length;
        }
        return encoded;
    }

    interface TCP {
        static PayloadEncoder newPayloadEncoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(cipherMethod, hkdfsha1(key, salt), NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadEncoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE, 0x3fff);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(cipherMethod, hkdfsha1(key, salt), NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            AEADChunkSizeParser sizeCodec = new AEADChunkSizeParser(auth);
            return new PayloadDecoder(auth, sizeCodec, EmptyPaddingLengthGenerator.INSTANCE);
        }
    }

    interface UDP {
        static PayloadEncoder newPayloadEncoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(cipherMethod, hkdfsha1(key, salt), NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
            return new PayloadEncoder(auth, EmptyChunkSizeParser.INSTANCE, EmptyPaddingLengthGenerator.INSTANCE, 0x3fff);
        }

        static PayloadDecoder newPayloadDecoder(CipherMethod cipherMethod, byte[] key, byte[] salt) {
            Authenticator auth = new Authenticator(cipherMethod, hkdfsha1(key, salt), NonceGenerator.generateInitialAEADNonce(), BytesGenerator.generateEmptyBytes());
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
