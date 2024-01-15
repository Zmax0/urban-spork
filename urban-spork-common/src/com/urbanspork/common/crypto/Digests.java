package com.urbanspork.common.crypto;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.Blake3Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

import java.util.function.Supplier;

public enum Digests {

    md5(MD5Digest::new),
    sha256(SHA256Digest::new),
    blake3(Blake3Digest::new),
    ;

    private final Supplier<Digest> provider;

    Digests(Supplier<Digest> provider) {
        this.provider = provider;
    }

    public byte[] hash(byte[] in) {
        Digest digest = provider.get();
        digest.update(in, 0, in.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

    public void hash(byte[] in, byte[] out, int outOff) {
        Digest digest = provider.get();
        digest.update(in, 0, in.length);
        digest.doFinal(out, outOff);
    }
}
