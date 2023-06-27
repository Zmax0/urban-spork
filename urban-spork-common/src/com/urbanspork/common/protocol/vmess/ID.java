package com.urbanspork.common.protocol.vmess;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.digests.MD5Digest;

import java.util.UUID;

public class ID {
    private ID() {}

    public static byte[][] newID(String[] uuids) {
        byte[][] ids = new byte[uuids.length][];
        for (int i = 0; i < uuids.length; i++) {
            ids[i] = newID(uuids[i]);
        }
        return ids;
    }

    public static byte[] newID(String uuid) {
        return newID(UUID.fromString(uuid));
    }

    public static byte[] newID(UUID uuid) {
        return newID(getBytes(uuid));
    }

    public static byte[] newID(byte[] uuid) {
        byte[] salt = "c48619fe-8f02-49e0-b9e9-edf763e17e21".getBytes();
        MD5Digest digest = new MD5Digest();
        digest.update(uuid, 0, uuid.length);
        digest.update(salt, 0, salt.length);
        byte[] newID = new byte[digest.getDigestSize()];
        digest.doFinal(newID, 0);
        return newID;
    }

    private static byte[] getBytes(UUID uuid) {
        byte[] uid = new byte[16];
        ByteBuf buf = Unpooled.wrappedBuffer(uid);
        buf.setLong(0, uuid.getMostSignificantBits());
        buf.setLong(Long.BYTES, uuid.getLeastSignificantBits());
        return uid;
    }

    public static byte[] nextID(byte[] oldID) {
        MD5Digest digest = new MD5Digest();
        digest.update(oldID, 0, oldID.length);
        byte[] salt = "16167dc8-16b6-4e6d-b8bb-65dd68113a81".getBytes();
        digest.update(salt, 0, salt.length);
        byte[] newID = new byte[oldID.length];
        digest.doFinal(newID, 0);
        return newID;
    }

    public static byte[][] newAlterIDs(byte[] id, int alterIDCount) {
        byte[][] alterIDs = new byte[alterIDCount][];
        byte[] pre = id;
        for (int i = 0; i < alterIDCount; i++) {
            byte[] nextID = nextID(pre);
            alterIDs[i] = newID(nextID);
            pre = nextID;
        }
        return alterIDs;
    }
}
