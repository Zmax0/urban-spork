package com.urbanspork.common.protocol.shadowsocks;

import java.util.concurrent.ThreadLocalRandom;

public class Session {

    private long packetId;
    private long clientSessionId;
    private long serverSessionId;

    public Session(long packetId, long clientSessionId, long serverSessionId) {
        this.packetId = packetId;
        this.clientSessionId = clientSessionId;
        this.serverSessionId = serverSessionId;
    }

    public static Session from(StreamType streamType) {
        long clientSessionId = 0;
        long serverSessionId = 0;
        if (StreamType.Request == streamType) {
            clientSessionId = ThreadLocalRandom.current().nextLong();
        } else {
            serverSessionId = ThreadLocalRandom.current().nextLong();
        }
        return new Session(1, clientSessionId, serverSessionId);
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

    public void setPacketId(long packetId) {
        this.packetId = packetId;
    }

    public void setClientSessionId(long clientSessionId) {
        this.clientSessionId = clientSessionId;
    }

    public void setServerSessionId(long serverSessionId) {
        this.serverSessionId = serverSessionId;
    }

    @Override
    public String toString() {
        return String.format("PI:%d, CI:%d, SI:%d", packetId, clientSessionId, serverSessionId);
    }
}
