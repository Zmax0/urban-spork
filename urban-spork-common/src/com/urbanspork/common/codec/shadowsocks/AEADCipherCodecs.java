package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethods;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.manage.shadowsocks.ServerUserManager;
import com.urbanspork.common.protocol.shadowsocks.aead.AEAD;
import com.urbanspork.common.protocol.shadowsocks.aead2022.AEAD2022;

import java.util.List;

public class AEADCipherCodecs {

    private AEADCipherCodecs() {}

    static AEADCipherCodec get(ServerConfig config) {
        List<ServerUserConfig> user = config.getUser();
        if (user != null) {
            user.stream().map(ServerUser::from).forEach(ServerUserManager.DEFAULT::addUser);
        }
        CipherKind kind = config.getCipher();
        Keys keys = passwordToKeys(kind, config.getPassword());
        if (CipherKind.chacha20_poly1305 == kind) {
            return new AEADCipherCodec(kind, CipherMethods.CHACHA20_POLY1305.get(), keys);
        } else {
            return new AEADCipherCodec(kind, CipherMethods.AES_GCM.get(), keys);
        }
    }

    static Keys passwordToKeys(CipherKind kind, String password) {
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
}
