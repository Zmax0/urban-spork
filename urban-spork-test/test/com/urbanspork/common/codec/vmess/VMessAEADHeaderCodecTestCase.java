package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.aead.AEADCipherCodecs;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VMess - AEAD Header Codec")
class VMessAEADHeaderCodecTestCase {

    @Test
    void testSealAndOpen() throws Exception {
        byte[] header = "Test Header".getBytes();
        byte[] key = KDF.kdf16("Demo Key for Auth ID Test".getBytes(), "Demo Path for Auth ID Test".getBytes());
        VMessAEADHeaderCodec codec = new VMessAEADHeaderCodec(AEADCipherCodecs.AES_GCM.get());
        ByteBuf in = Unpooled.buffer();
        codec.sealVMessAEADHeader(key, header, in);
        Assertions.assertTrue(in.isReadable());
        ByteBuf out = Unpooled.buffer();
        codec.openVMessAEADHeader(key, in, out);
        Assertions.assertFalse(in.isReadable());
        Assertions.assertTrue(out.isReadable());
        Assertions.assertArrayEquals(header, ByteBufUtil.getBytes(out));
    }

}
