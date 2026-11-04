package com.urbanspork.common.protocol.vmess.aead;

import com.urbanspork.common.crypto.AES;
import com.urbanspork.common.lang.Go;
import com.urbanspork.common.protocol.vmess.VMess;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

public class AuthID {
    private AuthID() {}

    private static final byte[] KDF_SALT_AUTH_ID_ENCRYPTION_KEY = "AES Auth ID Encryption".getBytes();

    public static byte[] createAuthID(byte[] key, long time) {
        byte[] authID = new byte[16];
        ByteBuf buf = Unpooled.wrappedBuffer(authID);
        buf.writerIndex(0);
        buf.writeLong(time);
        buf.writeBytes(Go.nextUnsignedInt());
        long crc32 = VMess.crc32(ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.writerIndex(), false));
        buf.writeInt((int) crc32);
        AES.encrypt128(KDF.kdf16(key, KDF_SALT_AUTH_ID_ENCRYPTION_KEY), authID);
        return authID;
    }

    public static byte[] match(byte[] authID, byte[][] keys) {
        for (byte[] key : keys) {
            byte[] bytes;
            try {
                bytes = Arrays.copyOf(authID, authID.length);
                AES.decrypt128(KDF.kdf16(key, KDF_SALT_AUTH_ID_ENCRYPTION_KEY), bytes);
            } catch (Exception _) {
                continue;
            }
            ByteBuf buf = Unpooled.wrappedBuffer(bytes);
            if (buf.getInt(12) == (int) VMess.crc32(ByteBufUtil.getBytes(buf, 0, 12, false))
                && Math.abs(buf.getLong(0) - VMess.now()) <= 120
            ) {
                // not support replay check now
                return key;
            }
        }
        return new byte[0];
    }
}