package com.urbanspork.common.protocol.vmess.aead;

import com.urbanspork.common.crypto.AES;
import com.urbanspork.common.golang.Golang;
import com.urbanspork.common.protocol.vmess.VMessProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.util.Arrays;

public class AuthID {

    public static byte[] createAuthID(byte[] key, long time) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] authID = new byte[16];
        ByteBuf buf = Unpooled.wrappedBuffer(authID);
        buf.writerIndex(0);
        buf.writeLong(time);
        buf.writeBytes(Golang.nextUnsignedInt());
        int crc32 = (int) VMessProtocol.crc32(ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.writerIndex(), false));
        buf.writeInt(crc32);
        return AES.ECB_NoPadding.encrypt(KDF.kdf16(key, VMessProtocol.KDF_SALT_AUTH_ID_ENCRYPTION_KEY), authID);
    }

    public static boolean match(byte[] authID, byte[] key) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] decrypt = AES.ECB_NoPadding.decrypt(KDF.kdf16(key, VMessProtocol.KDF_SALT_AUTH_ID_ENCRYPTION_KEY), authID);
        ByteBuf buf = Unpooled.wrappedBuffer(decrypt);
        long time = buf.readLong();
        if (Math.abs(time - Instant.now().getEpochSecond()) > 120) {
            return false;
        }
        buf.readUnsignedInt(); // rand
        int crc32 = buf.readInt();
        // TODO check replay
        return crc32 == (int) VMessProtocol.crc32(Arrays.copyOf(decrypt, decrypt.length - Integer.BYTES));
    }

}