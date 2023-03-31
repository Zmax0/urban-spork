package com.urbanspork.common.crypto;

import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

import java.util.function.Supplier;

public enum GeneralDigests {

    md5(MD5Digest::new),
    sha256(SHA256Digest::new);

    private final Supplier<GeneralDigest> provider;

    GeneralDigests(Supplier<GeneralDigest> provider) {
        this.provider = provider;
    }

    public byte[] get(byte[] in) {
        GeneralDigest digest = provider.get();
        digest.update(in, 0, in.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

}
