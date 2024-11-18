package com.urbanspork.test;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.util.Dice;

import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public interface TestDice {

    static String rollString(int length) {
        String str = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-=_+";
        Random random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(str.charAt(random.nextInt(str.length())));
        }
        return sb.toString();
    }

    static String rollString() {
        return rollString(256);
    }

    static CipherKind rollCipher() {
        CipherKind[] ciphers = CipherKind.values();
        return ciphers[ThreadLocalRandom.current().nextInt(0, ciphers.length)];
    }

    static String rollHost() {
        return rollString(10) + ".io";
    }

    static int rollPort() {
        return ThreadLocalRandom.current().nextInt(49152, 65535);
    }

    static String rollPassword(Protocol protocol, CipherKind kind) {
        if (Protocol.shadowsocks == protocol && kind.isAead2022()) {
            return rollAEAD2022Password(kind);
        } else {
            return UUID.randomUUID().toString();
        }
    }

    private static String rollAEAD2022Password(CipherKind kind) {
        return switch (kind) {
            case aead2022_blake3_aes_128_gcm -> Base64.getEncoder().encodeToString(Dice.rollBytes(16));
            case aead2022_blake3_aes_256_gcm, aead2022_blake3_chacha8_poly1305, aead2022_blake3_chacha20_poly1305 -> Base64.getEncoder().encodeToString(Dice.rollBytes(32));
            default -> throw new UnsupportedOperationException();
        };
    }
}
