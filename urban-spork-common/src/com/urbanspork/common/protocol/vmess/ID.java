package com.urbanspork.common.protocol.vmess;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.digests.MD5Digest;

import java.util.Arrays;
import java.util.UUID;

public class ID {

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
        byte[] salt1 = "16167dc8-16b6-4e6d-b8bb-65dd68113a81".getBytes();
        byte[] salt2 = "533eff8a-4113-4b10-b5ce-0f5d76b98cd2".getBytes();
        digest.update(salt1, 0, salt1.length);
        byte[] newID = new byte[oldID.length];
        for (; ; ) {
            digest.doFinal(newID, 0);
            if (!Arrays.equals(oldID, newID)) {
                return newID;
            }
            digest.update(salt2, 0, salt2.length);
        }
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
