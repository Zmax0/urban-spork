package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.vmess.VMessAEADHeaderCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class HeaderTestCase {

    @Test
    public void testOpenVMessAEADHeader() throws Exception {
        String header = "Test Header";
        byte[] key = AuthIDTestCase.createKey();
        byte[] testHeader = header.getBytes();
        VMessAEADHeaderCodec codec = newVMessAEADHeaderCodec();
        ByteBuf in = Unpooled.buffer();
        in.writeBytes(testHeader);
        ByteBuf out = Unpooled.buffer();
        codec.sealVMessAEADHeader(key, in, out);
        Assertions.assertFalse(in.isReadable());
        Assertions.assertTrue(out.isReadable());
        List<Object> list = new ArrayList<>();
        codec.openVMessAEADHeader(key, out, list);
        Assertions.assertFalse(out.isReadable());
        Assertions.assertFalse(list.isEmpty());
        if (list.get(0) instanceof ByteBuf buf) {
            Assertions.assertTrue(buf.isReadable());
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            Assertions.assertEquals(header, new String(bytes));
        }
    }

    private static VMessAEADHeaderCodec newVMessAEADHeaderCodec() {
        return new VMessAEADHeaderCodec() {
            @Override
            public AEADCipher cipher() {
                return new GCMBlockCipher(new AESEngine());
            }

            @Override
            public int macSize() {
                return 128;
            }

            @Override
            public int nonceSize() {
                return 12;
            }
        };
    }

}
