package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.manage.shadowsocks.ServerUser;

import java.util.concurrent.ThreadLocalRandom;

public class Control {
    private long clientSessionId;
    private long serverSessionId;
    private long packetId;
    private ServerUser user;

    public Control() {
        this(0, 0, 0, null);
    }

    public Control(long clientSessionId, long serverSessionId, long packetId, ServerUser user) {
        this.clientSessionId = clientSessionId;
        this.serverSessionId = serverSessionId;
        this.packetId = packetId;
        this.user = user;
    }

    public void increasePacketId(long i) {
        try {
            packetId = Math.addExact(packetId, i);
        } catch (ArithmeticException e) {
            long id;
            do {
                id = ThreadLocalRandom.current().nextLong();
            } while (id == this.clientSessionId);
            this.clientSessionId = id;
            this.packetId = 0;
        }
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
        return String.format("PI:%d, CI:%d, SI:%d", packetId, clientSessionId, serverSessionId);
    }
}
