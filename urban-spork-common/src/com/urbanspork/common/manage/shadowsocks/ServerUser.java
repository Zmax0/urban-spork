package com.urbanspork.common.manage.shadowsocks;

import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.crypto.Digests;
import com.urbanspork.common.util.ByteString;

import java.util.Arrays;
import java.util.Base64;

public record ServerUser(String name, byte[] key, byte[] identityHash) {
    public static ServerUser from(ServerUserConfig userConfig) {
        byte[] key = Base64.getDecoder().decode(userConfig.password());
        byte[] hash = Digests.blake3.hash(key);
        return new ServerUser(userConfig.name(), key, Arrays.copyOf(hash, 16));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServerUser that = (ServerUser) o;
        return Arrays.equals(identityHash, that.identityHash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(identityHash);
    }

    @Override
    public String toString() {
        return String.format("N:%s, K:%s, IH:%s", name, Base64.getEncoder().encodeToString(key), ByteString.valueOf(identityHash));
    }
}
