package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.util.ByteString;
import com.urbanspork.common.util.Dice;

public class Session {
    private final byte[] salt;
    private byte[] requestSalt;
    private ServerUser user;

    Session(byte[] salt, byte[] requestSalt) {
        this.salt = salt;
        this.requestSalt = requestSalt;
    }

    public Session(CipherKind kind) {
        this(Dice.rollBytes(kind.keySize()), null);
    }

    public byte[] salt() {
        return salt;
    }

    public byte[] getRequestSalt() {
        return requestSalt;
    }

    public void setRequestSalt(byte[] requestSalt) {
        this.requestSalt = requestSalt;
    }

    public ServerUser getUser() {
        return user;
    }

    public void setUser(ServerUser user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return String.format("S:%s, RS:%s", ByteString.valueOf(salt), ByteString.valueOf(requestSalt));
    }
}
