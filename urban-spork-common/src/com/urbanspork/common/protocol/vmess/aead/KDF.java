package com.urbanspork.common.protocol.vmess.aead;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.Arrays;

public class KDF {
    private KDF() {}

    private static final byte[] KDF_SALT_VMESS_AEAD_KDF = "VMess AEAD KDF".getBytes();

    public static byte[] kdf(byte[] key, byte[]... paths) {
        HMacCreator hmacCreator = new HMacCreator(KDF_SALT_VMESS_AEAD_KDF);
        if (paths != null) {
            for (byte[] path : paths) {
                hmacCreator = new HMacCreator(hmacCreator, path);
            }
        }
        Digest digest = hmacCreator.create();
        digest.update(key, 0, key.length);
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return result;
    }

    public static byte[] kdf(byte[] key, int len, byte[]... paths) {
        return len == 32 ? kdf(key, paths) : Arrays.copyOf(kdf(key, paths), len);
    }

    public static byte[] kdf16(byte[] key, byte[]... paths) {
        return kdf(key, 16, paths);
    }

    static class HMacCreator {

        HMacCreator parent;
        byte[] value;

        public HMacCreator(byte[] value) {
            this.value = value;
        }

        HMacCreator(HMacCreator parent, byte[] value) {
            this.parent = parent;
            this.value = value;
        }

        Digest create() {
            if (parent == null) {
                HMac init = new HMac(new SHA256Digest());
                init.init(new KeyParameter(value));
                return create(init);
            }
            HMac hmac = new HMac(parent.create());
            hmac.init(new KeyParameter(value));
            return create(hmac);
        }

        Digest create(HMac inner) {
            return new Digest() {
                @Override
                public String getAlgorithmName() {
                    return inner.getUnderlyingDigest().getAlgorithmName();
                }

                @Override
                public int getDigestSize() {
                    return inner.getUnderlyingDigest().getDigestSize();
                }

                @Override
                public void update(byte in) {
                    inner.update(in);
                }

                @Override
                public void update(byte[] in, int inOff, int len) {
                    inner.update(in, inOff, len);
                }

                @Override
                public int doFinal(byte[] out, int outOff) {
                    return inner.doFinal(out, outOff);
                }

                @Override
                public void reset() {
                    inner.reset();
                }
            };
        }
    }
}
