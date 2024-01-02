package com.urbanspork.common.codec.shadowsocks;

import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Keys(byte[] encKey, byte[][] identityKeys) {
    @Override
    public String toString() {
        return String.format("EK:%s, IK:%s",
            Base64.getEncoder().encodeToString(encKey),
            Stream.of(identityKeys).map(Base64.getEncoder()::encodeToString).collect(Collectors.joining("|")));
    }
}