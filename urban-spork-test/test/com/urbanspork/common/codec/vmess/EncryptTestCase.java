package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.protocol.vmess.aead.Encrypt;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

@DisplayName("VMess - Encrypt")
class EncryptTestCase {
    @Test
    void testSealAndOpen() throws Exception {
        byte[] header = "Test Header".getBytes();
        byte[] key = KDF.kdf16("Demo Key for Auth ID Test".getBytes(), "Demo Path for Auth ID Test".getBytes());
        ByteBuf in = Unpooled.buffer();
        Encrypt.sealVMessAEADHeader(key, header, in);
        Assertions.assertTrue(in.isReadable());
        Assertions.assertEquals(Unpooled.EMPTY_BUFFER, Encrypt.openVMessAEADHeader(key, in.slice(0, 57)));
        ByteBuf slice = in.readSlice(ThreadLocalRandom.current().nextInt(58, in.readableBytes()));
        Assertions.assertEquals(Unpooled.EMPTY_BUFFER, Encrypt.openVMessAEADHeader(key, slice));
        ByteBuf wrapped = Unpooled.wrappedBuffer(slice, in);
        ByteBuf out = Encrypt.openVMessAEADHeader(key, wrapped);
        Assertions.assertFalse(wrapped.isReadable());
        Assertions.assertTrue(out.isReadable());
        Assertions.assertArrayEquals(header, ByteBufUtil.getBytes(out));
    }
}
