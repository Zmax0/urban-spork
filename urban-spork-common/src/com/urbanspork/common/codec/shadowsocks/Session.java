package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.manage.shadowsocks.ServerUser;
import com.urbanspork.common.protocol.network.Network;
import com.urbanspork.common.util.ByteString;
import com.urbanspork.common.util.Dice;

import java.util.concurrent.ThreadLocalRandom;

public class Session {

    private final Network network;
    private final byte[] salt;
    private byte[] requestSalt;
    private ServerUser user;
    private long packetId;
    private long clientSessionId;
    private long serverSessionId;

    Session(Network network, long packetId, long clientSessionId, long serverSessionId, byte[] salt, byte[] requestSalt) {
        this.network = network;
        this.packetId = packetId;
        this.clientSessionId = clientSessionId;
        this.serverSessionId = serverSessionId;
        this.salt = salt;
        this.requestSalt = requestSalt;
    }

    public static Session tcp(CipherKind kind) {
        int length = kind.keySize();
        byte[] salt = Dice.rollBytes(length);
        return new Session(Network.TCP, 0, 0, 0, salt, null);
    }

    public static Session udp(Mode mode, CipherKind kind) {
        int length = kind.keySize();
        byte[] salt = Dice.rollBytes(length);
        long clientSessionId = 0;
        long serverSessionId = 0;
        if (Mode.Client == mode) {
            clientSessionId = ThreadLocalRandom.current().nextLong();
        } else {
            serverSessionId = ThreadLocalRandom.current().nextLong();
        }
        return new Session(Network.UDP, 1, clientSessionId, serverSessionId, salt, null);
    }

    public byte[] salt() {
        return salt;
    }

    public byte[] getRequestSalt() {
        return requestSalt;
    }

    public long getAndIncreasePacketId() {
        return packetId++;
    }

    public long getPacketId() {
        return packetId;
    }

    public long getClientSessionId() {
        return clientSessionId;
    }

    public long getServerSessionId() {
        return serverSessionId;
    }

    public ServerUser getUser() {
        return user;
    }

    public void setRequestSalt(byte[] requestSalt) {
        this.requestSalt = requestSalt;
    }

    public void setPacketId(long packetId) {
        this.packetId = packetId;
    }

    public void setClientSessionId(long clientSessionId) {
        this.clientSessionId = clientSessionId;
    }

    public void setServerSessionId(long serverSessionId) {
        this.serverSessionId = serverSessionId;
    }

    public void setUser(ServerUser user) {
        this.user = user;
    }

    @Override
    public String toString() {
        if (Network.UDP == network) {
            return String.format("PI:%d, CI:%d, SI:%d", packetId, clientSessionId, serverSessionId);
        } else {
            return String.format("S:%s, RS:%s", ByteString.valueOf(salt), ByteString.valueOf(requestSalt));
        }
    }
}
