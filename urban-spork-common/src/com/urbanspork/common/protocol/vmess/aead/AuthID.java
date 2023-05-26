package com.urbanspork.common.protocol.vmess.aead;

import com.urbanspork.common.crypto.AES;
import com.urbanspork.common.lang.Go;
import com.urbanspork.common.protocol.vmess.VMess;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;

public class AuthID {
    private AuthID() {}

    private static final byte[] KDF_SALT_AUTH_ID_ENCRYPTION_KEY = "AES Auth ID Encryption".getBytes();

    public static byte[] createAuthID(byte[] key, long time) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] authID = new byte[16];
        ByteBuf buf = Unpooled.wrappedBuffer(authID);
        buf.writerIndex(0);
        buf.writeLong(time);
        buf.writeBytes(Go.nextUnsignedInt());
        int crc32 = (int) VMess.crc32(ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.writerIndex(), false));
        buf.writeInt(crc32);
        return AES.ECB_NoPadding.encrypt(KDF.kdf16(key, KDF_SALT_AUTH_ID_ENCRYPTION_KEY), authID);
    }
}