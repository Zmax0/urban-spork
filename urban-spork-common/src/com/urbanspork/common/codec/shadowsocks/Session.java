package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.util.ByteString;
import com.urbanspork.common.util.Dice;

import java.util.concurrent.ThreadLocalRandom;

public class Session extends Control {

    private final Network network;
    private final byte[] salt;
    private byte[] requestSalt;

    Session(Network network, long packetId, long clientSessionId, long serverSessionId, byte[] salt, byte[] requestSalt) {
        super(clientSessionId, serverSessionId, packetId, null);
        this.network = network;
        this.salt = salt;
        this.requestSalt = requestSalt;
    }

    public static Session tcp(CipherKind kind) {
        int length = kind.keySize();
        byte[] salt = Dice.rollBytes(length);
        return new Session(Network.TCP, 0, 0, 0, salt, null);
    }

    public static Session udp(CipherKind kind) {
        int length = kind.keySize();
        byte[] salt = Dice.rollBytes(length);
        long clientSessionId = ThreadLocalRandom.current().nextLong();
        long serverSessionId = ThreadLocalRandom.current().nextLong();
        return new Session(Network.UDP, 1, clientSessionId, serverSessionId, salt, null);
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


    @Override
    public String toString() {
        if (Network.UDP == network) {
            return super.toString();
        } else {
            return String.format("S:%s, RS:%s", ByteString.valueOf(salt), ByteString.valueOf(requestSalt));
        }
    }
}
