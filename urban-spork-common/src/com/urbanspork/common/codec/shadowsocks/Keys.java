package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.protocol.shadowsocks.aead.AEAD;
import com.urbanspork.common.protocol.shadowsocks.aead2022.AEAD2022;

import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Keys(byte[] encKey, byte[][] identityKeys) {
    public static Keys from(CipherKind kind, String password) {
        int keySize = kind.keySize();
        Keys keys;
        if (kind.isAead2022()) {
            keys = AEAD2022.passwordToKeys(password);
        } else {
            keys = new Keys(AEAD.opensslBytesToKey(password.getBytes(), keySize), new byte[][]{});
        }
        if (keys.encKey().length != keySize) {
            String msg = String.format("%s is expecting a %d bytes key, but password: %s (%d bytes after decode)",
                kind, keySize, password, keys.encKey().length);
            throw new IllegalArgumentException(msg);
        }
        return keys;
    }

    @Override
    public String toString() {
        return String.format("EK:%s, IK:%s",
            Base64.getEncoder().encodeToString(encKey),
            Stream.of(identityKeys).map(Base64.getEncoder()::encodeToString).collect(Collectors.joining("|")));
    }
}