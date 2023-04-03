package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.aead.AEADCipherCodecs;
import com.urbanspork.common.codec.aead.AuthIDTestCase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@DisplayName("VMess - AEAD Header Codec")
public class VMessAEADHeaderCodecTestCase {

    @Test
    public void testSealAndOpen() throws Exception {
        byte[] header = "Test Header".getBytes();
        byte[] key = AuthIDTestCase.createKey();
        VMessAEADHeaderCodec codec = new VMessAEADHeaderCodec(AEADCipherCodecs.AES_GCM.get());
        ByteBuf out = Unpooled.buffer();
        codec.sealVMessAEADHeader(key, header, out);
        Assertions.assertTrue(out.isReadable());
        List<Object> list = new ArrayList<>();
        codec.openVMessAEADHeader(key, out, list);
        Assertions.assertFalse(out.isReadable());
        Assertions.assertFalse(list.isEmpty());
        if (list.get(0) instanceof ByteBuf buf) {
            Assertions.assertTrue(buf.isReadable());
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            Assertions.assertArrayEquals(header, bytes);
        }
    }

}
